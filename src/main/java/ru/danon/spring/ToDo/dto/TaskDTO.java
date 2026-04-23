package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskDTO {
    private Integer id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private String priority;
    private Integer authorId;
    private List<TagDTO> tags;
    private List<Integer> tagIds;
    private List<String> tagNames;
}
