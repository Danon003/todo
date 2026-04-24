package ru.danon.spring.ToDo.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import ru.danon.spring.ToDo.dto.PersonDTO;
import ru.danon.spring.ToDo.dto.AuthenticationDTO;
import ru.danon.spring.ToDo.dto.ForgotPasswordRequest;
import ru.danon.spring.ToDo.dto.ResetPasswordRequest;
import ru.danon.spring.ToDo.mappers.PersonMapper;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.security.JWTUtil;
import ru.danon.spring.ToDo.services.RegistrationService;
import ru.danon.spring.ToDo.controllers.validators.PersonValidator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private PersonValidator personValidator;

    @Mock
    private RegistrationService registrationService;

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private PersonMapper personMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthController authController;

    @Test
    void shouldRegisterUser() {
        PersonDTO dto = new PersonDTO();
        dto.setUsername("test");
        dto.setPassword("123");
        dto.setEmail("test@mail.com");

        when(personMapper.toEntity(any())).thenReturn(new Person());
        when(jwtUtil.generateToken("test")).thenReturn("token");

        BindingResult bindingResult = new BeanPropertyBindingResult(dto, "personDTO");
        var response = authController.performRegistration(dto, bindingResult);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("token", response.getBody().getJwtToken());
        verify(registrationService).register(any());
    }

    @Test
    void shouldReturnBadRequestWhenPasswordIsMissing() {
        PersonDTO dto = new PersonDTO();
        dto.setUsername("test");
        dto.setPassword(" ");

        BindingResult bindingResult = new BeanPropertyBindingResult(dto, "personDTO");
        var response = authController.performRegistration(dto, bindingResult);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(personMapper, registrationService, jwtUtil);
    }

    @Test
    void shouldReturnBadRequestWhenValidationHasErrors() {
        PersonDTO dto = new PersonDTO();
        dto.setUsername("test");
        dto.setPassword("123");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);
        when(personMapper.toEntity(any())).thenReturn(new Person());

        var response = authController.performRegistration(dto, bindingResult);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(registrationService, never()).register(any());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void shouldPerformLogin() {
        AuthenticationDTO dto = new AuthenticationDTO();
        dto.setUsername("test");
        dto.setPassword("123");
        when(jwtUtil.generateToken("test")).thenReturn("jwt");

        var response = authController.performLogin(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt", response.getBody().getJwtToken());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void shouldInitiateForgotPassword() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@mail.com");

        var response = authController.performForgotPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(registrationService).initiatePasswordReset("test@mail.com");
    }

    @Test
    void shouldResetPassword() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@mail.com");
        request.setCode("1111");
        request.setNewPassword("new-password");

        var response = authController.performResetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(registrationService).resetPassword("test@mail.com", "1111", "new-password");
    }
}