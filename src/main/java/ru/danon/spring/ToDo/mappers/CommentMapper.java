package ru.danon.spring.ToDo.mappers;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.danon.spring.ToDo.dto.CommentDTO;
import ru.danon.spring.ToDo.models.mongo.Comment;

@Component
@RequiredArgsConstructor
public class CommentMapper {
    private final ModelMapper modelMapper;

    public CommentDTO toDTO(Comment comment) {
        return modelMapper.map(comment, CommentDTO.class);
    }
    public Comment toEntity(CommentDTO commentDTO) {return modelMapper.map(commentDTO, Comment.class);}

}
