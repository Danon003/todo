package ru.danon.spring.ToDo.dto;

import lombok.Data;

@Data
public class GradeRequest {
    private Integer grade;
    private String comment;
}
