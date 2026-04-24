package ru.danon.spring.ToDo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.danon.spring.ToDo.dto.LogResponseDTO;
import ru.danon.spring.ToDo.models.postgre.Group;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.models.postgre.RoleAuditLog;
import ru.danon.spring.ToDo.repositories.jpa.GroupRepository;
import ru.danon.spring.ToDo.repositories.jpa.RoleAuditLogRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskAssignmentRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskRepository;
import ru.danon.spring.ToDo.services.impl.AdminServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private PeopleService peopleService;
    @Mock private GroupRepository groupRepository;
    @Mock private NotificationProducerService notificationProducerServiceImpl;
    @Mock private RoleAuditLogRepository roleAuditLogRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private TaskService taskServiceImpl;
    @Mock private TaskAssignmentRepository taskAssignmentRepository;
    @Mock private GroupService groupServiceImpl;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void getAllUsersShouldDelegateToPeopleService() {
        PageRequest page = PageRequest.of(0, 5);
        when(peopleService.findAll(page)).thenReturn(new PageImpl<>(List.of(new Person()), page, 1));

        var result = adminService.getAllUsers(page);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void createGroupShouldPersistGroup() {
        Group saved = new Group();
        saved.setId(10L);
        when(groupRepository.save(any(Group.class))).thenReturn(saved);

        adminService.createGroup("A-01", "desc");

        ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(captor.capture());
        assertEquals("A-01", captor.getValue().getName());
        assertEquals("desc", captor.getValue().getDescription());
        assertNotNull(captor.getValue().getCreatedAt());
    }

    @Test
    void getUsersByRoleShouldPrefixRole() {
        PageRequest page = PageRequest.of(0, 5);
        when(peopleService.findByRole("ROLE_STUDENT", page))
                .thenReturn(new PageImpl<>(List.of(new Person()), page, 1));

        var result = adminService.getUsersByRole("STUDENT", page);

        assertEquals(1, result.getTotalElements());
        verify(peopleService).findByRole("ROLE_STUDENT", page);
    }

    @Test
    void getRoleAuditLogsShouldMapLogsToDto() {
        RoleAuditLog log = new RoleAuditLog();
        LogResponseDTO dto = new LogResponseDTO();
        when(roleAuditLogRepository.findAll()).thenReturn(List.of(log));
        when(modelMapper.map(log, LogResponseDTO.class)).thenReturn(dto);

        List<LogResponseDTO> result = adminService.getRoleAuditLogs();

        assertEquals(1, result.size());
        assertSame(dto, result.getFirst());
    }

    @Test
    void createTeacherShouldUpdateRoleAndSendNotification() {
        Person person = new Person();
        person.setId(7L);
        person.setRole("ROLE_STUDENT");
        when(peopleService.findById(7L)).thenReturn(Optional.of(person));
        when(peopleService.save(person)).thenReturn(person);

        Person result = adminService.createTeacher(7L);

        assertEquals("ROLE_TEACHER", result.getRole());
        verify(notificationProducerServiceImpl).sendChangeRoleNotification(7L, "TEACHER");
        verify(roleAuditLogRepository).save(any(RoleAuditLog.class));
    }
}
