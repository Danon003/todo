package ru.danon.spring.ToDo.dto.kanban;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class KanbanDayDTO {
    private LocalDate date;
    private List<KanbanItemDTO> items;
}
