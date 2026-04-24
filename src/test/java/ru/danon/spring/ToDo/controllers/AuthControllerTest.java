package ru.danon.spring.ToDo.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;
import ru.danon.spring.ToDo.TestSecurityConfig;
import ru.danon.spring.ToDo.controllers.validators.PersonValidator;
import ru.danon.spring.ToDo.dto.AuthenticationDTO;
import ru.danon.spring.ToDo.dto.PersonDTO;
import ru.danon.spring.ToDo.mappers.PersonMapper;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.security.JWTUtil;
import ru.danon.spring.ToDo.security.PersonDetailsService;
import ru.danon.spring.ToDo.services.RegistrationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PersonValidator personValidator;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private JWTUtil jwtUtil;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private PersonMapper personMapper;

    @MockitoBean
    private PersonDetailsService personDetailsService;

    @Test
    void performRegistration_Success() throws Exception {
        PersonDTO personDTO = new PersonDTO();
        personDTO.setUsername("testuser");
        personDTO.setEmail("test@example.com");
        personDTO.setPassword("password123");

        Person person = new Person();
        person.setUsername("testuser");
        person.setEmail("test@example.com");

        when(personMapper.toEntity(any(PersonDTO.class))).thenReturn(person);
        when(jwtUtil.generateToken(anyString())).thenReturn("test-jwt-token");
        doNothing().when(personValidator).validate(any(Person.class), any(BindingResult.class));

        mockMvc.perform(post("/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(personDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt-token").value("test-jwt-token"));

        verify(registrationService).register(any(Person.class));
        verify(jwtUtil).generateToken("testuser");
    }

    @Test
    void performRegistration_ValidationError() throws Exception {
        PersonDTO personDTO = new PersonDTO();
        personDTO.setUsername("te");
        personDTO.setEmail("invalid-email");
        personDTO.setPassword("123");

        Person person = new Person();
        when(personMapper.toEntity(any(PersonDTO.class))).thenReturn(person);

        doAnswer(invocation -> {
            BindingResult bindingResult = invocation.getArgument(1);
            bindingResult.reject("error", "Validation error");
            return null;
        }).when(personValidator).validate(any(Person.class), any(BindingResult.class));

        mockMvc.perform(post("/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(personDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Error!"));
    }

    @Test
    void performRegistration_EmptyPassword_ReturnsError() throws Exception {
        PersonDTO personDTO = new PersonDTO();
        personDTO.setUsername("testuser");
        personDTO.setEmail("test@example.com");
        personDTO.setPassword("");

        Person person = new Person();
        when(personMapper.toEntity(any(PersonDTO.class))).thenReturn(person);

        mockMvc.perform(post("/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(personDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Пароль обязателен для регистрации!"));

        verify(registrationService, never()).register(any());
    }

    @Test
    void performLogin_Success() throws Exception {
        AuthenticationDTO authDTO = new AuthenticationDTO();
        authDTO.setUsername("testuser");
        authDTO.setPassword("password123");

        when(jwtUtil.generateToken(anyString())).thenReturn("test-jwt-token");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(UsernamePasswordAuthenticationToken.class));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt-token").value("test-jwt-token"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken("testuser");
    }

    @Test
    void performLogin_BadCredentials() throws Exception {
        AuthenticationDTO authDTO = new AuthenticationDTO();
        authDTO.setUsername("testuser");
        authDTO.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Incorrect credentials!"));
    }
}