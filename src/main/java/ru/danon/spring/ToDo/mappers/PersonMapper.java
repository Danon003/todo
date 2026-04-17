package ru.danon.spring.ToDo.mappers;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.danon.spring.ToDo.dto.AboutUserResponseDTO;
import ru.danon.spring.ToDo.dto.PersonDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.models.postgre.Person;

@Component
@RequiredArgsConstructor
public class PersonMapper {
    private final ModelMapper modelMapper;

    public Person toEntity(PersonDTO personDTO) {
        return modelMapper.map(personDTO, Person.class);
    }

    public PersonResponseDTO toDto(Person person) {
        return modelMapper.map(person, PersonResponseDTO.class);
    }

    public AboutUserResponseDTO toAboutUserDto(Person person) {
        return new AboutUserResponseDTO(person.getUsername());
    }
}
