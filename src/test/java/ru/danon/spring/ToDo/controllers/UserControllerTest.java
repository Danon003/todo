package ru.danon.spring.ToDo.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.danon.spring.ToDo.dto.AboutUserResponseDTO;
import ru.danon.spring.ToDo.dto.GroupResponseDTO;
import ru.danon.spring.ToDo.dto.PersonDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.mappers.PersonMapper;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.PeopleService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private PeopleService peopleService;
    @Mock
    private GroupService groupService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PersonMapper personMapper;

    @InjectMocks
    private UserController userController;

    @Test
    void getUserInfoShouldReturnCurrentUserInfo() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user1");
        PersonResponseDTO dto = new PersonResponseDTO();
        dto.setUsername("user1");
        when(peopleService.getUserInfo("user1")).thenReturn(dto);

        var response = userController.getUserInfo(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("user1", response.getBody().getUsername());
    }

    @Test
    void getMyGroupShouldReturnGroupInfo() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("student");
        GroupResponseDTO group = new GroupResponseDTO();
        group.setId(3L);
        when(groupService.getGroupInfo(auth)).thenReturn(group);

        var response = userController.getMyGroup(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3L, response.getBody().getId());
    }

    @Test
    void getAboutUserShouldReturnDtoWhenUserExists() {
        Person person = new Person();
        person.setUsername("teacher");
        AboutUserResponseDTO about = new AboutUserResponseDTO("teacher");
        when(peopleService.findById(7L)).thenReturn(Optional.of(person));
        when(personMapper.toAboutUserDto(person)).thenReturn(about);

        var response = userController.getAboutUser(7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("teacher", response.getBody().getTeacherName());
    }

    @Test
    void getAboutUserShouldThrowWhenUserNotFound() {
        when(peopleService.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userController.getAboutUser(99L));
    }

    @Test
    void updateProfileShouldEncodePasswordWhenProvided() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user1");
        PersonDTO dto = new PersonDTO();
        dto.setUsername("new-name");
        dto.setEmail("new@mail.com");
        dto.setPassword("pass123");
        when(passwordEncoder.encode("pass123")).thenReturn("encoded-pass");
        PersonResponseDTO responseDto = new PersonResponseDTO();
        when(peopleService.updateUserProfile("user1", "new-name", "new@mail.com", "encoded-pass"))
                .thenReturn(responseDto);

        var response = userController.updateProfile(dto, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(passwordEncoder).encode("pass123");
    }

    @Test
    void updateProfileShouldPassNullPasswordWhenNotProvided() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user1");
        PersonDTO dto = new PersonDTO();
        dto.setUsername("new-name");
        dto.setEmail("new@mail.com");
        dto.setPassword(" ");
        PersonResponseDTO responseDto = new PersonResponseDTO();
        when(peopleService.updateUserProfile("user1", "new-name", "new@mail.com", null))
                .thenReturn(responseDto);

        var response = userController.updateProfile(dto, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
