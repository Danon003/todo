package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TaskStatDTO {
    private Long id;
    private String status;
    private Map<String, Long> statusStatistics;
    private Long userId;
    private Long groupId;
}
