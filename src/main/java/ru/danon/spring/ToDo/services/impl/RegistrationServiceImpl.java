package ru.danon.spring.ToDo.services.impl;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.NotificationEvent;
import ru.danon.spring.ToDo.enums.EmailTemplate;
import ru.danon.spring.ToDo.enums.RegistrationMessage;
import ru.danon.spring.ToDo.exceptions.RegistrationException;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.repositories.jpa.PeopleRepository;
import ru.danon.spring.ToDo.services.NotificationProducerService;
import ru.danon.spring.ToDo.services.RegistrationService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Service
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {
    private final PeopleRepository peopleRepository;
    private final NotificationProducerService notificationProducerServiceImpl;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    // In-memory хранилище для токенов (можно заменить на Redis в будущем)
    private final Map<String, PasswordResetToken> resetTokens = new ConcurrentHashMap<>();

    private static final int TOKEN_EXPIRATION_MINUTES = 30;

    @Transactional
    @Override
    public void register(Person person) {
        log.info("Регистрация нового пользователя: username={}, email={}", person.getUsername(), person.getEmail());

        // Проверка username
        if (peopleRepository.findByUsername(person.getUsername()).isPresent()) {
            log.warn("Попытка регистрации с существующим username: {}", person.getUsername());
            throw new RegistrationException(RegistrationMessage.USERNAME_TAKEN.getMessage());
        }

        // Проверка email
        if (peopleRepository.findByEmail(person.getEmail()).isPresent()) {
            log.warn("Попытка регистрации с существующим email: {}", person.getEmail());
            throw new RegistrationException(RegistrationMessage.EMAIL_TAKEN.getMessage());
        }

        person.setPassword(passwordEncoder.encode(person.getPassword()));
        person.setRole("ROLE_STUDENT");
        peopleRepository.save(person);

        Person person1 = peopleRepository.findByUsername(person.getUsername()).orElseThrow();
        log.info("Пользователь успешно зарегистрирован: id={}, username={}", person1.getId(), person1.getUsername());

        //уведомление: зарегистрирован новый пользователь
        notificationProducerServiceImpl.sendNotification(
                new NotificationEvent(
                        UUID.randomUUID().toString(),
                        "REGISTER",
                        "Вы успешно зарегистрировались!",
                        RegistrationMessage.REGISTRATION_SUCCESS.getMessage(),
                        person1.getId(),
                        "ROLE_STUDENT",
                        Timestamp.valueOf(LocalDateTime.now()),
                        Map.of("userId", person1.getId())
                )
        );
        log.debug("Уведомление о регистрации отправлено пользователю id={}", person1.getId());
    }

    @Transactional
    @Override
    public void updatePasswordsToBCrypt() {
        log.info("Обновление паролей на BCrypt");
        List<Person> persons = peopleRepository.findAll();
        int updatedCount = 0;
        for (Person person : persons) {
            if (!person.getPassword().startsWith("$2a$")) {
                person.setPassword(passwordEncoder.encode(person.getPassword()));
                updatedCount++;
            }
        }
        peopleRepository.saveAll(persons);
        log.info("Обновлено {} паролей на BCrypt", updatedCount);
    }

    /**
     * Инициирует процесс сброса пароля
     */
    @Override
    public void initiatePasswordReset(@NotBlank(message = "Email не может быть пустым")
                                      @Email(message = "Некорректный формат email") String email) {

        String normalizedEmail = email.toLowerCase().trim();
        log.info("Запрос на сброс пароля для email: {}", normalizedEmail);

        Optional<Person> personOptional = peopleRepository.findByEmail(normalizedEmail);

        if (personOptional.isPresent()) {
            Person person = personOptional.get();

            // Генерируем токен
            String resetToken = generateResetToken();
            LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES);

            // Удаляем старые токены для этого email
            cleanupOldTokensForEmail(normalizedEmail);

            // Сохраняем новый токен в памяти
            PasswordResetToken token = new PasswordResetToken(resetToken, normalizedEmail, expiryDate);
            resetTokens.put(generateTokenKey(normalizedEmail, resetToken), token);

            // Отправляем email с токеном
            sendPasswordResetEmail(person.getEmail(), resetToken, person.getUsername());

            // Очищаем просроченные токены
            cleanupExpiredTokens();

            log.info("Токен сброса пароля сгенерирован и отправлен для email: {}", normalizedEmail);
        } else {
            // Для безопасности не сообщаем, что пользователь не найден
            log.warn("Попытка сброса пароля для несуществующего email: {}", normalizedEmail);
        }
    }

    /**
     * Сбрасывает пароль пользователя
     */
    @Transactional
    @Override
    public void resetPassword(@NotBlank(message = "Email не может быть пустым")
                              @Email(message = "Некорректный формат email") String email,
                              @NotBlank(message = "Код подтверждения не может быть пустым") String code,
                              @NotBlank(message = "Пароль не может быть пустым")
                              @Size(min = 4, message = "Пароль должен содержать минимум 4 символов") String newPassword) {

        String normalizedEmail = email.toLowerCase().trim();
        String normalizedCode = code.trim();
        log.info("Сброс пароля для email: {}", normalizedEmail);

        // Проверка пароля
        if (newPassword == null || newPassword.length() < 4) {
            throw new RegistrationException(RegistrationMessage.INVALID_PASSWORD.getMessage());
        }

        // Находим пользователя
        Person user = peopleRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.error("Пользователь с email {} не найден при сбросе пароля", normalizedEmail);
                    return new RuntimeException(RegistrationMessage.USER_NOT_FOUND.getMessage());
                });

        // Проверяем токен
        String tokenKey = generateTokenKey(normalizedEmail, normalizedCode);
        PasswordResetToken resetToken = resetTokens.get(tokenKey);

        if (resetToken == null) {
            log.warn("Неверный код подтверждения для email: {}", normalizedEmail);
            throw new IllegalArgumentException(RegistrationMessage.INVALID_RESET_CODE.getMessage());
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            // Удаляем просроченный токен
            resetTokens.remove(tokenKey);
            log.warn("Просроченный код подтверждения для email: {}", normalizedEmail);
            throw new IllegalArgumentException(RegistrationMessage.RESET_CODE_EXPIRED.getMessage());
        }

        // Обновляем пароль
        user.setPassword(passwordEncoder.encode(newPassword));
        peopleRepository.save(user);

        // Удаляем использованный токен
        resetTokens.remove(tokenKey);

        // Удаляем все токены для этого email (на всякий случай)
        cleanupOldTokensForEmail(normalizedEmail);

        // Отправляем уведомление об успешном сбросе пароля
        sendPasswordResetSuccessEmail(user.getEmail(), user.getUsername());

        // Отправляем системное уведомление
        sendPasswordResetNotification(user);

        log.info("Пароль успешно сброшен для пользователя: {}", user.getUsername());
    }

    /**
     * Проверяет валидность токена сброса пароля
     */
    @Override
    public boolean validateResetToken(String token, String email) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            String tokenKey = generateTokenKey(normalizedEmail, token.trim());

            PasswordResetToken resetToken = resetTokens.get(tokenKey);

            if (resetToken == null) {
                log.debug("Токен не найден для email: {}", normalizedEmail);
                return false;
            }

            boolean isValid = resetToken.getExpiryDate().isAfter(LocalDateTime.now());

            // Если токен просрочен, удаляем его
            if (!isValid) {
                resetTokens.remove(tokenKey);
                log.debug("Токен просрочен для email: {}", normalizedEmail);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Ошибка при валидации токена сброса пароля", e);
            return false;
        }
    }

    /**
     * Генерирует уникальный токен для сброса пароля
     */
    private String generateResetToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * Генерирует ключ для хранения токена в Map
     */
    private String generateTokenKey(String email, String token) {
        return email + ":" + token;
    }

    /**
     * Удаляет старые токены для указанного email
     */
    private void cleanupOldTokensForEmail(String email) {
        resetTokens.keySet().removeIf(key -> key.startsWith(email + ":"));
        log.debug("Очищены старые токены для email: {}", email);
    }

    /**
     * Очищает просроченные токены
     */
    private void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int beforeSize = resetTokens.size();
        resetTokens.entrySet().removeIf(entry -> entry.getValue().getExpiryDate().isBefore(now));
        int removed = beforeSize - resetTokens.size();
        if (removed > 0) {
            log.debug("Очищено {} просроченных токенов", removed);
        }
    }

    /**
     * Отправляет email с токеном сброса пароля
     */
    @Async
    protected void sendPasswordResetEmail(String email, String token, String username) {
        try {
            EmailTemplate template = EmailTemplate.PASSWORD_RESET;
            String body = template.formatBody(username, token, TOKEN_EXPIRATION_MINUTES);

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(email);
            mailMessage.setSubject(template.getSubject());
            mailMessage.setText(body);
            mailSender.send(mailMessage);

            log.info("Email для сброса пароля отправлен на {}", email);
        } catch (Exception e) {
            log.error("Ошибка отправки email для сброса пароля на {}", email, e);
        }
    }

    /**
     * Отправляет email об успешном сбросе пароля
     */
    @Async
    protected void sendPasswordResetSuccessEmail(String email, String username) {
        try {
            EmailTemplate template = EmailTemplate.PASSWORD_RESET_SUCCESS;
            String body = template.formatBody(username);

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(email);
            mailMessage.setSubject(template.getSubject());
            mailMessage.setText(body);
            mailSender.send(mailMessage);

            log.info("Email об успешном сбросе пароля отправлен на {}", email);
        } catch (Exception e) {
            log.error("Ошибка отправки email об успешном сбросе пароля на {}", email, e);
        }
    }

    /**
     * Отправляет системное уведомление о смене пароля
     */
    private void sendPasswordResetNotification(Person user) {
        try {
            notificationProducerServiceImpl.sendNotification(
                    new NotificationEvent(
                            UUID.randomUUID().toString(),
                            "SYSTEM",
                            "Пароль изменен",
                            "Ваш пароль был успешно изменен через систему восстановления.",
                            user.getId(),
                            user.getRole(),
                            Timestamp.valueOf(LocalDateTime.now()),
                            Map.of("userId", user.getId(), "action", "password_reset")
                    )
            );
            log.debug("Системное уведомление о смене пароля отправлено пользователю id={}", user.getId());
        } catch (Exception e) {
            log.error("Ошибка отправки системного уведомления о смене пароля для пользователя id={}", user.getId(), e);
        }
    }

    /**
     * Внутренний класс для хранения информации о токене
     */
    @Getter
    @Setter
    private static class PasswordResetToken {
        private final String token;
        private final String email;
        private final LocalDateTime expiryDate;

        public PasswordResetToken(String token, String email, LocalDateTime expiryDate) {
            this.token = token;
            this.email = email;
            this.expiryDate = expiryDate;
        }
    }
}
