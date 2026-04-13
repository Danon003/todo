package ru.danon.spring.ToDo.dto;

import lombok.Data;

@Data
public class CommentRequest {
    private String content;
    private String parentId;
}