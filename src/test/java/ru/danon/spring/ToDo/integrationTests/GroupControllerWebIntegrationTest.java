package ru.danon.spring.ToDo.integrationTests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.danon.spring.ToDo.TestSecurityConfig;
import ru.danon.spring.ToDo.controllers.GroupController;
import ru.danon.spring.ToDo.dto.GroupResponseDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.mappers.GroupMapper;
import ru.danon.spring.ToDo.security.JWTUtil;
import ru.danon.spring.ToDo.security.PersonDetailsService;
import ru.danon.spring.ToDo.services.AdminService;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.TaskService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GroupController.class)
@Import({TestSecurityConfig.class, GroupController.class})
class GroupControllerWebIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupService groupService;
    @MockitoBean
    private AdminService adminService;
    @MockitoBean
    private TaskService taskService;
    @MockitoBean
    private GroupMapper groupMapper;
    @MockitoBean
    private JWTUtil jwtUtil;
    @MockitoBean
    private PersonDetailsService personDetailsService;

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void getAllGroupsShouldReturnPage() throws Exception {
        GroupResponseDTO dto = new GroupResponseDTO();
        dto.setId(1L);
        dto.setName("A-01");
        when(groupService.findAll(any(), any())).thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/group"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("A-01"));
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void studentsGroupShouldReturnStudents() throws Exception {
        PersonResponseDTO student = new PersonResponseDTO();
        student.setId(10L);
        student.setUsername("student1");
        when(groupService.getStudentsByGroupId(1L)).thenReturn(List.of(student));

        mockMvc.perform(get("/group/1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].username").value("student1"));
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void createGroupShouldReturnOkForAdmin() throws Exception {
        doNothing().when(adminService).createGroup("A-02", "desc");

        mockMvc.perform(post("/group")
                        .param("name", "A-02")
                        .param("description", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void deleteGroupShouldBeForbiddenForTeacher() throws Exception {
        mockMvc.perform(delete("/group/2"))
                .andExpect(status().isForbidden());
    }
}
