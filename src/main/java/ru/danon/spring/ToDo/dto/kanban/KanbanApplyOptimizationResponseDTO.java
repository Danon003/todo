package ru.danon.spring.ToDo.dto.kanban;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class KanbanApplyOptimizationResponseDTO {
    private Integer appliedCount;
    private List<KanbanItemDTO> updatedItems;
}
