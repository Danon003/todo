package ru.danon.spring.ToDo.services;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.events.NotificationEvent;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.repositories.jpa.PeopleRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final PeopleRepository peopleRepository;
    private final NotificationProducerService notificationProducerService;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    // In-memory хранилище для токенов (можно заменить на Redis в будущем)
    private final Map<String, PasswordResetToken> resetTokens = new ConcurrentHashMap<>();

    private static final int TOKEN_EXPIRATION_MINUTES = 30;

    @Transactional
    public void register(Person person) {
        person.setPassword(passwordEncoder.encode(person.getPassword()));
        person.setRole("ROLE_STUDENT");
        peopleRepository.save(person);

        Person person1 = peopleRepository.findByUsername(person.getUsername()).orElseThrow();
        //уведомление: зарегистрирован новый пользователь
        notificationProducerService.sendNotification(
                new NotificationEvent(
                        UUID.randomUUID().toString(),
                        "REGISTER",
                        "Вы успешно зарегистрировались!",
                        "Спасибо за регистрацию!",
                        person1.getId(),
                        "ROLE_STUDENT",
                        Timestamp.valueOf(LocalDateTime.now()),
                        Map.of("userId", person1.getId())
                )
        );
    }

    @Transactional
    public void updatePasswordsToBCrypt() {
        List<Person> persons = peopleRepository.findAll();
        for (Person person : persons) {
            if (!person.getPassword().startsWith("$2a$")) {
                person.setPassword(passwordEncoder.encode(person.getPassword()));
            }
        }
        peopleRepository.saveAll(persons);
    }

    /**
     * Инициирует процесс сброса пароля
     */
    public void initiatePasswordReset(@NotBlank(message = "Email не может быть пустым")
                                      @Email(message = "Некорректный формат email") String email) {

        String normalizedEmail = email.toLowerCase().trim();

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
        } else {
            // Для безопасности не сообщаем, что пользователь не найден
            // Просто логируем и выходим
        }
    }

    /**
     * Сбрасывает пароль пользователя
     */
    @Transactional
    public void resetPassword(@NotBlank(message = "Email не может быть пустым")
                              @Email(message = "Некорректный формат email") String email,
                              @NotBlank(message = "Код подтверждения не может быть пустым") String code,
                              @NotBlank(message = "Пароль не может быть пустым")
                              @Size(min = 4, message = "Пароль должен содержать минимум 4 символов") String newPassword) {

        String normalizedEmail = email.toLowerCase().trim();
        String normalizedCode = code.trim();

        // Находим пользователя
        Person user = peopleRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверяем токен
        String tokenKey = generateTokenKey(normalizedEmail, normalizedCode);
        PasswordResetToken resetToken = resetTokens.get(tokenKey);

        if (resetToken == null) {
            throw new RuntimeException("Неверный код подтверждения");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            // Удаляем просроченный токен
            resetTokens.remove(tokenKey);
            throw new RuntimeException("Срок действия кода истек");
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
    }

    /**
     * Проверяет валидность токена сброса пароля
     */
    public boolean validateResetToken(String token, String email) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            String tokenKey = generateTokenKey(normalizedEmail, token.trim());

            PasswordResetToken resetToken = resetTokens.get(tokenKey);

            if (resetToken == null) {
                return false;
            }

            boolean isValid = resetToken.getExpiryDate().isAfter(LocalDateTime.now());

            // Если токен просрочен, удаляем его
            if (!isValid) {
                resetTokens.remove(tokenKey);
            }

            return isValid;
        } catch (Exception e) {
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
    }

    /**
     * Очищает просроченные токены
     */
    private void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        resetTokens.entrySet().removeIf(entry -> entry.getValue().getExpiryDate().isBefore(now));
    }

    /**
     * Отправляет email с токеном сброса пароля
     */
    @Async
    protected void sendPasswordResetEmail(String email, String token, String username) {
        try {
            String subject = "Восстановление пароля";
            String message = String.format("""
                Здравствуйте, %s!
                
                Для восстановления пароля используйте следующий код подтверждения:
                
                %s
                
                Код действителен в течение %d минут.
                
                Если вы не запрашивали восстановление пароля, проигнорируйте это письмо.
                
                С уважением,
                Команда поддержки
                """, username, token, TOKEN_EXPIRATION_MINUTES);

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(email);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);
            mailSender.send(mailMessage);
        } catch (Exception e) {
            // Логируем ошибку, но не прерываем выполнение
            System.err.println("Ошибка отправки email: " + e.getMessage());
        }
    }

    /**
     * Отправляет email об успешном сбросе пароля
     */
    @Async
    protected void sendPasswordResetSuccessEmail(String email, String username) {
        try {
            String subject = "Пароль успешно изменен";
            String message = String.format("""
                Здравствуйте, %s!
                
                Ваш пароль был успешно изменен.
                
                Если это были не вы, пожалуйста, немедленно свяжитесь со службой поддержки.
                
                С уважением,
                Команда поддержки
                """, username);

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(email);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);
            mailSender.send(mailMessage);
        } catch (Exception e) {
            System.err.println("Ошибка отправки email: " + e.getMessage());
        }
    }

    /**
     * Отправляет системное уведомление о смене пароля
     */
    private void sendPasswordResetNotification(Person user) {
        try {
            notificationProducerService.sendNotification(
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
        } catch (Exception e) {
            System.err.println("Ошибка отправки уведомления: " + e.getMessage());
        }
    }

    /**
     * Внутренний класс для хранения информации о токене
     */
    private static class PasswordResetToken {
        private final String token;
        private final String email;
        private final LocalDateTime expiryDate;

        public PasswordResetToken(String token, String email, LocalDateTime expiryDate) {
            this.token = token;
            this.email = email;
            this.expiryDate = expiryDate;
        }

        public String getToken() { return token; }
        public String getEmail() { return email; }
        public LocalDateTime getExpiryDate() { return expiryDate; }
    }
}