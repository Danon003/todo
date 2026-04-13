package ru.danon.spring.ToDo.models.mongo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "comments")
public class Comment {
    @Id
    private String id;
    private Integer taskId;
    private Integer authorId;
    private String authorName;
    private String authorRole;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String parentId; // для будущих ответов
    private Integer repliesCount = 0;


    public Comment() {}

    public Comment(Integer taskId, Integer authorId, String authorName, String authorRole, String content) {
        this.taskId = taskId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}