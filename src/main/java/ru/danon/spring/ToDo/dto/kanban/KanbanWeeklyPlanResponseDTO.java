package ru.danon.spring.ToDo.dto.kanban;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class KanbanWeeklyPlanResponseDTO {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private Integer totalPlannedTasks;
    private Integer capacityPerDay;
    private List<KanbanWeeklyPlanDayDTO> days;
    private List<String> globalActions;
}
