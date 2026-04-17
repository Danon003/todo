package ru.danon.spring.ToDo.mappers;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.danon.spring.ToDo.dto.TaskFileDTO;
import ru.danon.spring.ToDo.models.postgre.TaskFile;

@Component
@RequiredArgsConstructor
public class TaskFileMapper {
    private final ModelMapper modelMapper;

    public TaskFileDTO toDTO(TaskFile taskFile) {
        return modelMapper.map(taskFile, TaskFileDTO.class);
    }
}
