package ru.danon.spring.ToDo.dto.kanban;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class KanbanMoveRequestDTO {
    @NotNull
    private LocalDate scheduledDate;

    @NotNull
    @Min(0)
    private Integer position;
}
