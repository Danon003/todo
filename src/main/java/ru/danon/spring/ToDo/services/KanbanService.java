package ru.danon.spring.ToDo.services;

import ru.danon.spring.ToDo.dto.kanban.KanbanBoardResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanApplyOptimizationRequestDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanApplyOptimizationResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanInsightsResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanItemDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanMoveRequestDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanOptimizationResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanWeeklyPlanResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanWhatIfResponseDTO;

import java.time.LocalDate;

public interface KanbanService {
    KanbanBoardResponseDTO getBoard(String username, LocalDate startDate, LocalDate endDate);

    KanbanItemDTO move(Long kanbanTaskId, KanbanMoveRequestDTO request, String username);

    KanbanOptimizationResponseDTO optimize(String username, int dailyLimit, int bufferDays);

    KanbanApplyOptimizationResponseDTO applyOptimization(String username, KanbanApplyOptimizationRequestDTO request);

    KanbanInsightsResponseDTO getInsights(String username, LocalDate startDate, LocalDate endDate);

    KanbanWhatIfResponseDTO whatIf(String username, Integer minDailyLimit, Integer maxDailyLimit, Integer minBuffer, Integer maxBuffer);

    KanbanWeeklyPlanResponseDTO getWeeklyPlan(String username, LocalDate weekStart);
}
