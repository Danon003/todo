package ru.danon.spring.ToDo.dto.kanban;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class KanbanBoardResponseDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<KanbanDayDTO> days;
    private Integer totalItems;
    private Integer totalFixedItems;
}
