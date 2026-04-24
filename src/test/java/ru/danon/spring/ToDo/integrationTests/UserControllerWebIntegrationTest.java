package ru.danon.spring.ToDo.integrationTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.danon.spring.ToDo.TestSecurityConfig;
import ru.danon.spring.ToDo.controllers.UserController;
import ru.danon.spring.ToDo.dto.GroupResponseDTO;
import ru.danon.spring.ToDo.dto.PersonDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.mappers.PersonMapper;
import ru.danon.spring.ToDo.security.JWTUtil;
import ru.danon.spring.ToDo.security.PersonDetailsService;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.PeopleService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({TestSecurityConfig.class, UserController.class})
class UserControllerWebIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PeopleService peopleService;
    @MockitoBean
    private GroupService groupService;
    @MockitoBean
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private PersonMapper personMapper;
    @MockitoBean
    private JWTUtil jwtUtil;
    @MockitoBean
    private PersonDetailsService personDetailsService;

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void getUserInfoShouldReturnCurrentUser() throws Exception {
        PersonResponseDTO dto = new PersonResponseDTO();
        dto.setUsername("student1");
        dto.setEmail("student1@mail.com");
        when(peopleService.getUserInfo("student1")).thenReturn(dto);

        mockMvc.perform(get("/user/me/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("student1"))
                .andExpect(jsonPath("$.email").value("student1@mail.com"));
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void getMyGroupShouldReturnGroup() throws Exception {
        GroupResponseDTO group = new GroupResponseDTO();
        group.setId(2L);
        group.setName("A-01");
        when(groupService.getGroupInfo(any())).thenReturn(group);

        mockMvc.perform(get("/user/my-group"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("A-01"));
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void updateProfileShouldReturnUpdatedUser() throws Exception {
        PersonDTO request = new PersonDTO();
        request.setUsername("newname");
        request.setEmail("new@mail.com");
        request.setPassword("secret");

        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        PersonResponseDTO updated = new PersonResponseDTO();
        updated.setUsername("newname");
        updated.setEmail("new@mail.com");
        when(peopleService.updateUserProfile(eq("student1"), eq("newname"), eq("new@mail.com"), eq("encoded-secret")))
                .thenReturn(updated);

        mockMvc.perform(put("/user/me/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newname"))
                .andExpect(jsonPath("$.email").value("new@mail.com"));
    }
}
