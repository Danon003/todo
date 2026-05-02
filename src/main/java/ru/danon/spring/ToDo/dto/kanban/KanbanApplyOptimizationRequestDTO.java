package ru.danon.spring.ToDo.dto.kanban;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KanbanApplyOptimizationRequestDTO {
    @NotEmpty
    private List<@Valid KanbanApplyOptimizationItemDTO> items;
}
