package ru.danon.spring.ToDo.dto.kanban;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class KanbanInsightsResponseDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalPlanned;
    private Integer overloadedDays;
    private Integer riskTasksCount;
    private List<String> recommendations;
    private KanbanCoachingInsightDTO coaching;
}
