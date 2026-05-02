package ru.danon.spring.ToDo.dto.kanban;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class KanbanOptimizationResponseDTO {
    private List<KanbanOptimizationSuggestionDTO> suggestions;
    private List<String> recommendations;
    private Integer effectiveDailyLimit;
    private Integer overloadedDays;
    private Integer riskTasksCount;
    private KanbanCoachingInsightDTO coaching;
}
