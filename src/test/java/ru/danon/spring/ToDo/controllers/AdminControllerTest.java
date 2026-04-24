package ru.danon.spring.ToDo.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.danon.spring.ToDo.dto.DashboardStatsDTO;
import ru.danon.spring.ToDo.dto.PersonDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.mappers.PersonMapper;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.services.AdminService;
import ru.danon.spring.ToDo.services.PeopleService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;
    @Mock
    private PeopleService peopleService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PersonMapper personMapper;

    @InjectMocks
    private AdminController adminController;

    @Test
    void createUserShouldReturnBadRequestWhenEmailAlreadyExists() {
        PersonDTO dto = new PersonDTO();
        dto.setEmail("used@mail.com");
        when(peopleService.findByEmail("used@mail.com")).thenReturn(Optional.of(new Person()));

        var response = adminController.createUser(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(peopleService, never()).save(any());
    }

    @Test
    void createUserShouldCreateStudentWithEncodedPassword() {
        PersonDTO dto = new PersonDTO();
        dto.setEmail("new@mail.com");

        Person mapped = new Person();
        mapped.setPassword("plain");
        when(peopleService.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(personMapper.toEntity(dto)).thenReturn(mapped);
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(peopleService.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));
        when(personMapper.toDto(any(Person.class))).thenReturn(new PersonResponseDTO());

        var response = adminController.createUser(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        verify(peopleService).save(captor.capture());
        assertEquals("encoded", captor.getValue().getPassword());
        assertEquals("ROLE_STUDENT", captor.getValue().getRole());
        assertNotNull(captor.getValue().getCreatedAt());
    }

    @Test
    void deleteUserShouldReturnNoContent() {
        var response = adminController.deleteUser(5L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(peopleService).deleteById(5L);
    }

    @Test
    void changeUserRoleShouldUppercaseRole() {
        var response = adminController.changeUserRole("teacher", 10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(adminService).changeUserRole(10L, "TEACHER");
    }

    @Test
    void getAllUsersShouldMapUsersToDtoPage() {
        PageRequest page = PageRequest.of(0, 2);
        Person person = new Person();
        person.setId(1L);
        PersonResponseDTO dto = new PersonResponseDTO();
        dto.setId(1L);

        when(adminService.getAllUsers(page)).thenReturn(new PageImpl<>(List.of(person), page, 1));
        when(personMapper.toDto(person)).thenReturn(dto);

        var result = adminController.getAllUsers(page);

        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().getFirst().getId());
    }

    @Test
    void getStatisticShouldReturnStatsFromService() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        DashboardStatsDTO stats = new DashboardStatsDTO();
        when(adminService.getDashboardStats(auth)).thenReturn(stats);

        var response = adminController.getStatistic(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(stats, response.getBody());
    }
}
