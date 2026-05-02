package ru.danon.spring.ToDo.integrationTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.danon.spring.ToDo.TestSecurityConfig;
import ru.danon.spring.ToDo.controllers.AuthController;
import ru.danon.spring.ToDo.controllers.validators.PersonValidator;
import ru.danon.spring.ToDo.dto.AuthenticationDTO;
import ru.danon.spring.ToDo.dto.PersonDTO;
import ru.danon.spring.ToDo.mappers.PersonMapper;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.security.PersonDetailsService;
import ru.danon.spring.ToDo.security.JWTUtil;
import ru.danon.spring.ToDo.services.RegistrationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({TestSecurityConfig.class, AuthController.class})
class AuthControllerWebIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

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
    private PersonDetailsService personDetailsService;
    @MockitoBean
    private PersonMapper personMapper;
    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Test
    void registrationShouldReturnJwtToken() throws Exception {
        PersonDTO personDTO = new PersonDTO();
        personDTO.setUsername("testuser");
        personDTO.setEmail("test@example.com");
        personDTO.setPassword("pass1234");

        Person person = new Person();
        person.setUsername("testuser");
        when(personMapper.toEntity(any(PersonDTO.class))).thenReturn(person);
        when(jwtUtil.generateToken("testuser")).thenReturn("jwt-token");
        doNothing().when(personValidator).validate(any(), any());

        mockMvc.perform(post("/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(personDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt-token").value("jwt-token"));
    }

    @Test
    void loginShouldReturnJwtToken() throws Exception {
        AuthenticationDTO authDTO = new AuthenticationDTO();
        authDTO.setUsername("testuser");
        authDTO.setPassword("pass1234");

        when(jwtUtil.generateToken(anyString())).thenReturn("login-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt-token").value("login-token"));
    }
}
