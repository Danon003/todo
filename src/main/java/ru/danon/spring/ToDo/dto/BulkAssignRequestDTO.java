package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.util.List;

@Data
public class BulkAssignRequestDTO {
    private List<Long> taskIds;
    private List<Long> groupIds;
}
