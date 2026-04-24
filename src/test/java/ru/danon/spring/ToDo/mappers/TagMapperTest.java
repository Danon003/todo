package ru.danon.spring.ToDo.mappers;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import ru.danon.spring.ToDo.dto.TagDTO;
import ru.danon.spring.ToDo.models.postgre.Tag;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TagMapperTest {

    private final TagMapper tagMapper = new TagMapper(new ModelMapper());

    @Test
    void toDtoShouldMapFields() {
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("java");

        TagDTO dto = tagMapper.toDto(tag);

        assertEquals(1L, dto.getId());
        assertEquals("java", dto.getName());
    }

    @Test
    void toEntityShouldMapFields() {
        TagDTO dto = new TagDTO(2L, "spring");

        Tag tag = tagMapper.toEntity(dto);

        assertEquals(2L, tag.getId());
        assertEquals("spring", tag.getName());
    }

    @Test
    void toDtoListShouldReturnEmptyForNullInput() {
        assertTrue(tagMapper.toDtoList(null).isEmpty());
    }

    @Test
    void toDtoListShouldMapAllTags() {
        Tag first = new Tag();
        first.setId(1L);
        first.setName("a");
        Tag second = new Tag();
        second.setId(2L);
        second.setName("b");

        List<TagDTO> result = tagMapper.toDtoList(List.of(first, second));

        assertEquals(2, result.size());
        assertEquals("a", result.get(0).getName());
        assertEquals("b", result.get(1).getName());
    }
}
