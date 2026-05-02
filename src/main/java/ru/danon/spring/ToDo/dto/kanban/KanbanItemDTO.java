package ru.danon.spring.ToDo.dto.kanban;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class KanbanItemDTO {
    private Long id;
    private String itemType;
    private Long itemId;
    private String title;
    private LocalDate scheduledDate;
    private Integer position;
    private Boolean isFixed;
    private Boolean isOptimized;
    private LocalDateTime deadline;
    private String priority;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
