package ru.danon.spring.ToDo.dto.kanban;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class KanbanWeeklyPlanDayDTO {
    private LocalDate date;
    private Integer plannedTasks;
    private Integer capacity;
    private String loadLevel;
    private List<String> actions;
}
