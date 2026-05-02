package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private String priority;
    private Long authorId;
    private List<TagDTO> tags;
    private List<Long> tagIds;
    private List<String> tagNames;
}
