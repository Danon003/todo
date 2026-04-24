package ru.danon.spring.ToDo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.repositories.jpa.PeopleRepository;
import ru.danon.spring.ToDo.services.impl.PeopleServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeopleServiceImplTest {

    @Mock
    private PeopleRepository peopleRepository;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PeopleServiceImpl peopleService;

    @Test
    void getUserInfoShouldMapFoundUser() {
        Person person = new Person();
        person.setUsername("user1");
        PersonResponseDTO dto = new PersonResponseDTO();
        dto.setUsername("user1");
        when(peopleRepository.findByUsername("user1")).thenReturn(Optional.of(person));
        when(modelMapper.map(person, PersonResponseDTO.class)).thenReturn(dto);

        PersonResponseDTO result = peopleService.getUserInfo("user1");

        assertEquals("user1", result.getUsername());
    }

    @Test
    void updateUserProfileShouldThrowWhenCurrentUserNotFound() {
        when(peopleRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> peopleService.updateUserProfile("missing", "newUser", "new@mail.com", "pwd"));
    }

    @Test
    void updateUserProfileShouldThrowWhenEmailTakenByAnotherUser() {
        Person current = new Person();
        current.setId(1L);
        current.setUsername("old");
        current.setEmail("old@mail.com");
        Person another = new Person();
        another.setId(2L);

        when(peopleRepository.findByUsername("old")).thenReturn(Optional.of(current));
        when(peopleRepository.findByEmail("taken@mail.com")).thenReturn(Optional.of(another));

        assertThrows(IllegalArgumentException.class,
                () -> peopleService.updateUserProfile("old", "old", "taken@mail.com", null));
    }

    @Test
    void updateUserProfileShouldThrowWhenUsernameTakenByAnotherUser() {
        Person current = new Person();
        current.setId(1L);
        current.setUsername("old");
        current.setEmail("old@mail.com");
        Person another = new Person();
        another.setId(2L);
        another.setUsername("taken");

        when(peopleRepository.findByUsername("old")).thenReturn(Optional.of(current));
        when(peopleRepository.findByUsername("taken")).thenReturn(Optional.of(another));

        assertThrows(IllegalArgumentException.class,
                () -> peopleService.updateUserProfile("old", "taken", "old@mail.com", null));
    }

    @Test
    void updateUserProfileShouldUpdateAllFieldsWhenValid() {
        Person current = new Person();
        current.setId(1L);
        current.setUsername("old");
        current.setEmail("old@mail.com");
        current.setPassword("oldpwd");
        PersonResponseDTO dto = new PersonResponseDTO();
        dto.setUsername("new");
        dto.setEmail("new@mail.com");

        when(peopleRepository.findByUsername("old")).thenReturn(Optional.of(current));
        when(peopleRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(peopleRepository.findByUsername("new")).thenReturn(Optional.empty());
        when(peopleRepository.save(current)).thenReturn(current);
        when(modelMapper.map(current, PersonResponseDTO.class)).thenReturn(dto);

        PersonResponseDTO result = peopleService.updateUserProfile("old", "new", "new@mail.com", "newpwd");

        assertEquals("new", result.getUsername());
        assertEquals("new@mail.com", result.getEmail());
        assertEquals("newpwd", current.getPassword());
    }

    @Test
    void findByRolePageShouldDelegateToRepository() {
        PageRequest pageable = PageRequest.of(0, 5);
        when(peopleRepository.findByRole("ROLE_STUDENT", pageable))
                .thenReturn(new PageImpl<>(List.of(new Person()), pageable, 1));

        var result = peopleService.findByRole("ROLE_STUDENT", pageable);

        assertEquals(1, result.getTotalElements());
        verify(peopleRepository).findByRole("ROLE_STUDENT", pageable);
    }
}
