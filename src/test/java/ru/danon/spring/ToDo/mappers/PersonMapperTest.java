package ru.danon.spring.ToDo.mappers;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import ru.danon.spring.ToDo.dto.PersonDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.models.postgre.Person;

import static org.junit.jupiter.api.Assertions.*;

class PersonMapperTest {

    private final ModelMapper modelMapper = new ModelMapper();
    private final PersonMapper personMapper = new PersonMapper(modelMapper);

    @Test
    void toDto_ShouldMapAllFields() {
        // Given
        Person person = new Person();
        person.setId(1L);
        person.setUsername("testuser");
        person.setEmail("test@mail.com");
        person.setRole("ROLE_STUDENT");

        // When
        PersonResponseDTO dto = personMapper.toDto(person);

        // Then
        assertNotNull(dto);
        assertEquals(person.getId(), dto.getId());
        assertEquals(person.getUsername(), dto.getUsername());
        assertEquals(person.getEmail(), dto.getEmail());
        assertEquals(person.getRole(), dto.getRole());
    }

    @Test
    void toDto_ShouldThrowException_WhenPersonIsNull() {
        // Then
        assertThrows(IllegalArgumentException.class, () -> {
            personMapper.toDto(null);
        });
    }

    @Test
    void toEntity_ShouldMapAllFields() {
        // Given
        PersonDTO dto = new PersonDTO();
        dto.setUsername("newuser");
        dto.setEmail("new@mail.com");
        dto.setPassword("secret");

        // When
        Person person = personMapper.toEntity(dto);

        // Then
        assertNotNull(person);
        assertEquals(dto.getUsername(), person.getUsername());
        assertEquals(dto.getEmail(), person.getEmail());
        assertEquals(dto.getPassword(), person.getPassword());
    }

    @Test
    void toEntity_ShouldThrowException_WhenDtoIsNull() {
        // Then
        assertThrows(IllegalArgumentException.class, () -> {
            personMapper.toEntity(null);
        });
    }
}