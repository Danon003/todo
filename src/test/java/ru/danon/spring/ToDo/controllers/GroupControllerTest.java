package ru.danon.spring.ToDo.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import ru.danon.spring.ToDo.dto.GroupResponseDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.mappers.GroupMapper;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.services.AdminService;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.TaskService;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    @Mock
    private GroupService groupService;
    @Mock
    private AdminService adminService;
    @Mock
    private TaskService taskService;
    @Mock
    private GroupMapper groupMapper;

    @InjectMocks
    private GroupController groupController;

    @Test
    void studentsGroupShouldReturnEmptyListWhenGroupIdBlank() {
        var response = groupController.studentsGroup(" ");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(groupService, never()).getStudentsByGroupId(anyLong());
    }

    @Test
    void studentsGroupShouldReturnStudentsForValidId() {
        PersonResponseDTO student = new PersonResponseDTO();
        student.setId(1L);
        when(groupService.getStudentsByGroupId(2L)).thenReturn(List.of(student));

        var response = groupController.studentsGroup("2");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(1L, response.getBody().getFirst().getId());
    }

    @Test
    void getAllGroupsShouldReturnPageFromService() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("teacher");
        PageRequest page = PageRequest.of(0, 10);
        GroupResponseDTO group = new GroupResponseDTO();
        group.setId(10L);
        when(groupService.findAll(auth, page)).thenReturn(new PageImpl<>(List.of(group), page, 1));

        var result = groupController.getAllGroups(page, auth);

        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().getFirst().getId());
    }

    @Test
    void addStudentToGroupShouldReturnOk() {
        var response = groupController.addStudentToGroup(2L, 3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(groupService).addStudentToGroup(2L, 3L);
    }

    @Test
    void getGroupTasksShouldReturnTasksSet() {
        TaskResponseDTO task = new TaskResponseDTO();
        task.setId(100L);
        when(taskService.getGroupTasks(4L)).thenReturn(Set.of(task));

        var response = groupController.getGroupTasks(4L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getStudentsShouldMapTeacherStudentsToDto() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("teacher");
        Person person = new Person();
        PersonResponseDTO dto = new PersonResponseDTO();
        dto.setUsername("student");
        when(groupService.findByTeacherId(auth)).thenReturn(List.of(person));
        when(groupMapper.toDTOList(List.of(person))).thenReturn(List.of(dto));

        var response = groupController.getStudents(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("student", response.getBody().getFirst().getUsername());
    }
}
