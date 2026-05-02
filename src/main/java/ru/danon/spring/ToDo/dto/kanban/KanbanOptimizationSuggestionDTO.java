package ru.danon.spring.ToDo.dto.kanban;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class KanbanOptimizationSuggestionDTO {
    private Long kanbanTaskId;
    private LocalDate currentDate;
    private LocalDate suggestedDate;
    private Integer suggestedPosition;
    private String reason;
    private String riskLevel;
    private Integer score;
}
