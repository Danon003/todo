package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MyTaskDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private String priority;
    private Long authorId;
    private String userStatus;
    private List<TagDTO> tags;
    private List<Long> tagIds;
    private List<String> tagNames;

    public MyTaskDTO(Long id, String title, String description,
                     LocalDateTime deadline, String priority,
                     Long authorId, String userStatus, List<TagDTO> tags) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.priority = priority;
        this.authorId = authorId;
        this.userStatus = userStatus;
        this.tags = tags;
    }
}