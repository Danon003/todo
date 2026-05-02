package ru.danon.spring.ToDo.dto.kanban;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class KanbanWhatIfResponseDTO {
    private List<KanbanOptimizationResponseDTO> scenarios;
    private Integer bestScenarioDailyLimit;
    private Integer bestScenarioBufferDays;
    private String bestScenarioReason;
}
