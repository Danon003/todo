package ru.danon.spring.ToDo.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import ru.danon.spring.ToDo.dto.SolutionDTO;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.models.postgre.TaskAssignment;
import ru.danon.spring.ToDo.models.postgre.id.TaskAssignmentId;
import ru.danon.spring.ToDo.repositories.jpa.TaskAssignmentRepository;
import ru.danon.spring.ToDo.services.impl.FileStorageServiceImpl;
import ru.danon.spring.ToDo.services.impl.TaskServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock private PeopleService peopleService;
    @Mock private TaskAssignmentRepository taskAssignmentRepository;
    @Mock private FileStorageServiceImpl fileStorageService;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Person student;
    private Task task;
    private TaskAssignment assignment;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        student = new Person();
        student.setId(1L);
        student.setUsername("student");

        task = new Task();
        task.setId(100L);
        task.setTitle("Test Task");
        task.setDeadline(LocalDateTime.now().plusDays(1));

        assignment = new TaskAssignment();
        assignment.setTask(task);
        assignment.setUser(student);
        assignment.setSolutionFileName("solution.pdf");
        assignment.setSolutionFilePath("/path/to/file");
        assignment.setSolutionFileSize(1024L);

        auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("student");
    }

    @Test
    void getStudentSolution_ShouldReturnSolution_WhenExists() {
        // Given
        when(peopleService.findByUsername("student")).thenReturn(Optional.of(student));
        when(taskAssignmentRepository.findById(any(TaskAssignmentId.class)))
                .thenReturn(Optional.of(assignment));
        when(fileStorageService.generateDownloadUrl("/path/to/file"))
                .thenReturn("http://minio/solution.pdf");

        // When
        SolutionDTO result = taskService.getStudentSolution(100L, auth);

        // Then
        assertNotNull(result);
        assertEquals("solution.pdf", result.getFileName());
        assertEquals(1024L, result.getFileSize());
        assertEquals("http://minio/solution.pdf", result.getDownloadUrl());
    }

    @Test
    void getStudentSolution_ShouldReturnEmptyDto_WhenNoSolution() {
        // Given
        assignment.setSolutionFileName(null);
        assignment.setSolutionFilePath(null);
        assignment.setSolutionFileSize(null);

        when(peopleService.findByUsername("student")).thenReturn(Optional.of(student));
        when(taskAssignmentRepository.findById(any(TaskAssignmentId.class)))
                .thenReturn(Optional.of(assignment));

        // When
        SolutionDTO result = taskService.getStudentSolution(100L, auth);

        // Then
        assertNotNull(result);
        assertNull(result.getFileName());
        assertNull(result.getFileSize());
        assertNull(result.getDownloadUrl());
        verify(fileStorageService, never()).generateDownloadUrl(any());
    }

    @Test
    void getStudentSolution_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(peopleService.findByUsername("student")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class,
                () -> taskService.getStudentSolution(100L, auth));
    }

    @Test
    void getStudentSolution_ShouldThrowException_WhenAssignmentNotFound() {
        // Given
        when(peopleService.findByUsername("student")).thenReturn(Optional.of(student));
        when(taskAssignmentRepository.findById(any(TaskAssignmentId.class)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class,
                () -> taskService.getStudentSolution(100L, auth));
    }
}