package ru.danon.spring.ToDo.mappers;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import ru.danon.spring.ToDo.dto.CommentDTO;
import ru.danon.spring.ToDo.models.mongo.Comment;

import static org.junit.jupiter.api.Assertions.*;

class CommentMapperTest {

    private final CommentMapper commentMapper = new CommentMapper(new ModelMapper());

    @Test
    void toDTOShouldMapComment() {
        Comment comment = new Comment();
        comment.setId("c1");
        comment.setTaskId(10L);
        comment.setContent("hello");

        CommentDTO dto = commentMapper.toDTO(comment);

        assertEquals("c1", dto.getId());
        assertEquals(10L, dto.getTaskId());
        assertEquals("hello", dto.getContent());
    }

    @Test
    void toEntityShouldMapCommentDto() {
        CommentDTO dto = new CommentDTO();
        dto.setId("c2");
        dto.setAuthorName("student");
        dto.setContent("reply");

        Comment entity = commentMapper.toEntity(dto);

        assertEquals("c2", entity.getId());
        assertEquals("student", entity.getAuthorName());
        assertEquals("reply", entity.getContent());
    }
}
