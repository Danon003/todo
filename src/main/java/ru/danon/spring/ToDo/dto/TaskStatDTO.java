package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TaskStatDTO {
    private Integer id;
    private String status;
    private Map<String, Integer> statusStatistics;
    private Integer userId;
    private Integer groupId;
}
