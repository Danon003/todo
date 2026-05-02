package ru.danon.spring.ToDo.mappers;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import ru.danon.spring.ToDo.dto.GroupResponseDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.models.postgre.Group;
import ru.danon.spring.ToDo.models.postgre.Person;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroupMapperTest {

    private final GroupMapper groupMapper = new GroupMapper(new ModelMapper());

    @Test
    void toDTOShouldMapGroupFields() {
        Group group = new Group();
        group.setId(10L);
        group.setName("A1");
        group.setDescription("desc");

        GroupResponseDTO dto = groupMapper.toDTO(group);

        assertEquals(10L, dto.getId());
        assertEquals("A1", dto.getName());
        assertEquals("desc", dto.getDescription());
    }

    @Test
    void toDTOListShouldMapUsers() {
        Person one = new Person();
        one.setId(1L);
        one.setUsername("u1");
        Person two = new Person();
        two.setId(2L);
        two.setUsername("u2");

        List<PersonResponseDTO> result = groupMapper.toDTOList(List.of(one, two));

        assertEquals(2, result.size());
        assertEquals("u1", result.get(0).getUsername());
        assertEquals("u2", result.get(1).getUsername());
    }
}
