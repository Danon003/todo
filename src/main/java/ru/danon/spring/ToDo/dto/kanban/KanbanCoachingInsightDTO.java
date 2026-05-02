package ru.danon.spring.ToDo.dto.kanban;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KanbanCoachingInsightDTO {
    private Integer completedLast7Days;
    private Integer completedLast28Days;
    private Double avgCompletedPerWeek;
    private Integer overdueLast28Days;
    private Integer suggestedWeeklyLoad;
    private String burnoutRisk;
    private String advice;
}
