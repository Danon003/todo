package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MyTaskDTO {
    private Integer id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private String priority;
    private Integer authorId;
    private String userStatus;
    private List<TagDTO> tags;
    private List<Integer> tagIds;
    private List<String> tagNames;

    public MyTaskDTO(Integer id, String title, String description,
                     LocalDateTime deadline, String priority,
                     Integer authorId, String userStatus, List<TagDTO> tags) {
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