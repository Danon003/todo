package ru.danon.spring.ToDo.integrationTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.danon.spring.ToDo.TestSecurityConfig;
import ru.danon.spring.ToDo.controllers.TaskController;
import ru.danon.spring.ToDo.dto.MyTaskDTO;
import ru.danon.spring.ToDo.dto.StatusDTO;
import ru.danon.spring.ToDo.dto.TaskDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.mappers.TaskMapper;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.security.JWTUtil;
import ru.danon.spring.ToDo.security.PersonDetailsService;
import ru.danon.spring.ToDo.services.TagService;
import ru.danon.spring.ToDo.services.TaskService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TaskController.class)
@Import({TestSecurityConfig.class, TaskController.class})
class TaskControllerWebIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;
    @MockitoBean
    private TagService tagService;
    @MockitoBean
    private TaskMapper taskMapper;
    @MockitoBean
    private JWTUtil jwtUtil;
    @MockitoBean
    private PersonDetailsService personDetailsService;

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void createTaskShouldReturnCreatedTask() throws Exception {
        MyTaskDTO request = new MyTaskDTO(1L, "Task A", "desc", null, "HIGH", 1L, "NOT_STARTED", List.of());
        TaskResponseDTO response = new TaskResponseDTO();
        response.setId(100L);
        response.setTitle("Task A");
        when(taskService.createTask(any(MyTaskDTO.class), eq("teacher1"))).thenReturn(response);

        mockMvc.perform(post("/task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.title").value("Task A"));
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void getTasksShouldReturnPageForTeacher() throws Exception {
        Task task = new Task();
        task.setId(1L);
        TaskDTO dto = new TaskDTO();
        dto.setId(1L);
        when(taskService.findAllTasks(any())).thenReturn(new PageImpl<>(List.of(task), PageRequest.of(0, 20), 1));
        when(tagService.getTaskTagsBatch(List.of(1L))).thenReturn(Map.of());
        when(taskMapper.toDtoPage(any(), any())).thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/task"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void getMyTasksShouldReturnPageForStudent() throws Exception {
        MyTaskDTO myTask = new MyTaskDTO(5L, "My Task", "desc", null, "MEDIUM", 2L, "IN_PROGRESS", List.of());
        when(taskService.findMyTasks(eq("student1"), any())).thenReturn(new PageImpl<>(List.of(myTask), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/task/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5L))
                .andExpect(jsonPath("$.content[0].title").value("My Task"));
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void deleteTaskShouldReturnNoContent() throws Exception {
        doNothing().when(taskService).deleteTask(44L);

        mockMvc.perform(delete("/task/44"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "student1", roles = "STUDENT")
    void teacherEndpointShouldBeForbiddenForStudent() throws Exception {
        mockMvc.perform(delete("/task/44"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void updateTaskShouldReturnDto() throws Exception {
        TaskDTO request = new TaskDTO();
        request.setTitle("updated");
        Task updatedTask = new Task();
        TaskDTO response = new TaskDTO();
        response.setId(9L);
        response.setTitle("updated");
        when(taskService.updateTask(eq(9L), any(TaskDTO.class), eq("teacher1"))).thenReturn(updatedTask);
        when(taskMapper.toDto(updatedTask)).thenReturn(response);

        mockMvc.perform(put("/task/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9L))
                .andExpect(jsonPath("$.title").value("updated"));
    }
}
