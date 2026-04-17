package ru.danon.spring.ToDo.mappers;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.danon.spring.ToDo.dto.TagDTO;
import ru.danon.spring.ToDo.models.postgre.Tag;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TagMapper {

    private final ModelMapper modelMapper;

    public TagDTO toDto(Tag tag) {
        return modelMapper.map(tag, TagDTO.class);
    }

    public Tag toEntity(TagDTO dto) {
        return modelMapper.map(dto, Tag.class);
    }

    public List<TagDTO> toDtoList(List<Tag> tags) {
        if (tags == null) return List.of();
        return tags.stream()
                .map(this::toDto)
                .toList();
    }
}