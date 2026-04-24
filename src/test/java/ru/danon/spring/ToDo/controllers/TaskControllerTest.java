package ru.danon.spring.ToDo.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import ru.danon.spring.ToDo.dto.MyTaskDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.dto.StatusDTO;
import ru.danon.spring.ToDo.dto.TaskDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.mappers.TaskMapper;
import ru.danon.spring.ToDo.models.postgre.Tag;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.services.TagService;
import ru.danon.spring.ToDo.services.TaskService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;
    @Mock
    private TagService tagService;
    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskController taskController;

    @Test
    void createTaskShouldReturnCreatedTask() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("teacher");
        MyTaskDTO request = new MyTaskDTO(1L, "title", "desc", null, "HIGH", 1L, "NEW", List.of());
        TaskResponseDTO responseDto = new TaskResponseDTO();
        responseDto.setId(100L);
        when(taskService.createTask(request, "teacher")).thenReturn(responseDto);

        var result = taskController.createTask(request, auth);

        assertEquals(100L, result.getId());
    }

    @Test
    void getTasksShouldReturnEmptyPageWhenNoTasks() {
        PageRequest pageable = PageRequest.of(0, 5);
        when(taskService.findAllTasks(pageable)).thenReturn(Page.empty(pageable));

        var response = taskController.getTasks(pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verifyNoInteractions(tagService, taskMapper);
    }

    @Test
    void getTasksShouldReturnMappedPageWhenTasksExist() {
        PageRequest pageable = PageRequest.of(0, 5);
        Task task = new Task();
        task.setId(1L);
        Page<Task> tasks = new PageImpl<>(List.of(task), pageable, 1);
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setId(1L);
        Page<TaskDTO> dtoPage = new PageImpl<>(List.of(taskDTO), pageable, 1);
        Tag tag = new Tag();
        tag.setId(11L);
        tag.setName("java");

        when(taskService.findAllTasks(pageable)).thenReturn(tasks);
        when(tagService.getTaskTagsBatch(List.of(1L))).thenReturn(Map.of(1L, List.of(tag)));
        when(taskMapper.toDtoPage(tasks, Map.of(1L, List.of(tag)))).thenReturn(dtoPage);

        var response = taskController.getTasks(pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getContent().getFirst().getId());
    }

    @Test
    void assignTaskShouldDelegateToService() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("teacher");

        var response = taskController.assignTask(2L, 3L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(taskService).assignTask(2L, 3L, "teacher");
    }

    @Test
    void changeStatusMyTaskShouldReturnUpdatedTask() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("student");
        StatusDTO statusDTO = new StatusDTO("DONE");
        MyTaskDTO updated = new MyTaskDTO(1L, "title", "desc", null, "HIGH", 2L, "DONE", Collections.emptyList());
        when(taskService.changeMyTask(5L, "DONE", "student")).thenReturn(updated);

        var response = taskController.changeStatusMyTask(5L, statusDTO, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("DONE", response.getBody().getUserStatus());
    }

    @Test
    void updateTaskShouldMapAndReturnDto() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("teacher");
        TaskDTO request = new TaskDTO();
        Task task = new Task();
        TaskDTO mapped = new TaskDTO();
        mapped.setId(8L);
        when(taskService.updateTask(8L, request, "teacher")).thenReturn(task);
        when(taskMapper.toDto(task)).thenReturn(mapped);

        var response = taskController.updateTask(8L, request, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(8L, response.getBody().getId());
    }

    @Test
    void getListTaskShouldReturnUsers() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("student");
        PersonResponseDTO user = new PersonResponseDTO();
        user.setId(77L);
        when(taskService.getUsersWithTask(9L, auth)).thenReturn(List.of(user));

        var response = taskController.getListTask(9L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(77L, response.getBody().getFirst().getId());
    }
}
