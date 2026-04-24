package ru.danon.spring.ToDo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.danon.spring.ToDo.enums.RegistrationMessage;
import ru.danon.spring.ToDo.exceptions.RegistrationException;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.repositories.jpa.PeopleRepository;
import ru.danon.spring.ToDo.services.impl.RegistrationServiceImpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private PeopleRepository peopleRepository;
    @Mock
    private NotificationProducerService notificationProducerServiceImpl;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    void registerShouldThrowWhenUsernameTaken() {
        Person person = buildPerson(1L, "user1", "u@mail.com", "pass", "ROLE_STUDENT");
        when(peopleRepository.findByUsername("user1")).thenReturn(Optional.of(new Person()));

        RegistrationException ex = assertThrows(RegistrationException.class, () -> registrationService.register(person));
        assertEquals(RegistrationMessage.USERNAME_TAKEN.getMessage(), ex.getMessage());
    }

    @Test
    void registerShouldSaveUserAndSendNotification() {
        Person person = buildPerson(1L, "user1", "u@mail.com", "pass", "ROLE_STUDENT");
        when(peopleRepository.findByUsername("user1")).thenReturn(Optional.empty(), Optional.of(person));
        when(peopleRepository.findByEmail("u@mail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("encoded");

        registrationService.register(person);

        assertEquals("encoded", person.getPassword());
        assertEquals("ROLE_STUDENT", person.getRole());
        verify(peopleRepository).save(person);
        verify(notificationProducerServiceImpl).sendNotification(any());
    }

    @Test
    void updatePasswordsToBCryptShouldEncodeOnlyPlainPasswords() {
        Person plain = buildPerson(1L, "plain", "p@mail.com", "plainpass", "ROLE_STUDENT");
        Person bcrypt = buildPerson(2L, "bcrypt", "b@mail.com", "$2a$hash", "ROLE_STUDENT");
        when(peopleRepository.findAll()).thenReturn(List.of(plain, bcrypt));
        when(passwordEncoder.encode("plainpass")).thenReturn("$2a$encoded");

        registrationService.updatePasswordsToBCrypt();

        assertEquals("$2a$encoded", plain.getPassword());
        assertEquals("$2a$hash", bcrypt.getPassword());
        verify(peopleRepository).saveAll(List.of(plain, bcrypt));
    }

    @Test
    void initiatePasswordResetShouldCreateValidTokenForExistingEmail() {
        Person person = buildPerson(7L, "user1", "user@mail.com", "pass", "ROLE_STUDENT");
        when(peopleRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(person));

        registrationService.initiatePasswordReset(" User@mail.com ");

        Map<String, Object> tokens = getResetTokens();
        assertFalse(tokens.isEmpty());
        String token = tokens.keySet().iterator().next().split(":")[1];
        assertTrue(registrationService.validateResetToken(token, "user@mail.com"));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void resetPasswordShouldThrowWhenCodeInvalid() {
        Person person = buildPerson(7L, "user1", "user@mail.com", "pass", "ROLE_STUDENT");
        when(peopleRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(person));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrationService.resetPassword("user@mail.com", "BADCODE", "newpass"));

        assertEquals(RegistrationMessage.INVALID_RESET_CODE.getMessage(), ex.getMessage());
    }

    @Test
    void resetPasswordShouldUpdatePasswordAndCleanupToken() throws Exception {
        Person person = buildPerson(7L, "user1", "user@mail.com", "oldpass", "ROLE_STUDENT");
        when(peopleRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(person));
        when(passwordEncoder.encode("newpass")).thenReturn("encoded-new");

        putToken("user@mail.com", "ABCD1234", LocalDateTime.now().plusMinutes(5));
        registrationService.resetPassword("user@mail.com", "ABCD1234", "newpass");

        assertEquals("encoded-new", person.getPassword());
        verify(peopleRepository).save(person);
        assertFalse(registrationService.validateResetToken("ABCD1234", "user@mail.com"));
        verify(notificationProducerServiceImpl).sendNotification(any());
    }

    private Person buildPerson(Long id, String username, String email, String password, String role) {
        Person person = new Person();
        person.setId(id);
        person.setUsername(username);
        person.setEmail(email);
        person.setPassword(password);
        person.setRole(role);
        return person;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getResetTokens() {
        try {
            Field field = RegistrationServiceImpl.class.getDeclaredField("resetTokens");
            field.setAccessible(true);
            return (Map<String, Object>) field.get(registrationService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void putToken(String email, String code, LocalDateTime expiry) throws Exception {
        Class<?> tokenClass = Class.forName("ru.danon.spring.ToDo.services.impl.RegistrationServiceImpl$PasswordResetToken");
        Constructor<?> constructor = tokenClass.getDeclaredConstructor(String.class, String.class, LocalDateTime.class);
        constructor.setAccessible(true);
        Object token = constructor.newInstance(code, email, expiry);
        getResetTokens().put(email + ":" + code, token);
    }
}
