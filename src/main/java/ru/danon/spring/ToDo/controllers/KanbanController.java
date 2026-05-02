package ru.danon.spring.ToDo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.danon.spring.ToDo.dto.kanban.KanbanApplyOptimizationRequestDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanApplyOptimizationResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanBoardResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanInsightsResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanItemDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanMoveRequestDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanOptimizationResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanWeeklyPlanResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanWhatIfResponseDTO;
import ru.danon.spring.ToDo.services.KanbanService;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/kanban")
@Tag(name = "Kanban Controller", description = "Канбан-доска на 2 недели")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class KanbanController {
    private final KanbanService kanbanService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    @Operation(summary = "Получить доску", description = "Возвращает канбан-доску за период")
    public ResponseEntity<KanbanBoardResponseDTO> getBoard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication
    ) {
        log.info("Запрос на получение канбан-доски от {}", authentication.getName());
        return ResponseEntity.ok(kanbanService.getBoard(authentication.getName(), startDate, endDate));
    }

    @PutMapping("/{id}/move")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    @Operation(summary = "Переместить карточку", description = "Перемещает карточку в новый день/позицию")
    public ResponseEntity<KanbanItemDTO> move(
            @PathVariable Long id,
            @Valid @RequestBody KanbanMoveRequestDTO request,
            Authentication authentication
    ) {
        log.info("Запрос на перемещение kanbanTask id={} от {}", id, authentication.getName());
        return ResponseEntity.ok(kanbanService.move(id, request, authentication.getName()));
    }

    @PostMapping("/optimize")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    @Operation(summary = "Оптимизировать график", description = "Возвращает предложения без изменения базы")
    public ResponseEntity<KanbanOptimizationResponseDTO> optimize(
            @RequestParam(defaultValue = "4") Integer dailyLimit,
            @RequestParam(defaultValue = "1") Integer bufferDays,
            Authentication authentication
    ) {
        log.info("Запрос на оптимизацию канбана от {}", authentication.getName());
        return ResponseEntity.ok(kanbanService.optimize(authentication.getName(), dailyLimit, bufferDays));
    }

    @PostMapping("/optimize/apply")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    @Operation(summary = "Применить оптимизацию", description = "Применяет подтвержденные фронтом переносы")
    public ResponseEntity<KanbanApplyOptimizationResponseDTO> applyOptimization(
            @Valid @RequestBody KanbanApplyOptimizationRequestDTO request,
            Authentication authentication
    ) {
        log.info("Запрос на применение оптимизации канбана от {}", authentication.getName());
        return ResponseEntity.ok(kanbanService.applyOptimization(authentication.getName(), request));
    }

    @GetMapping("/insights")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    @Operation(summary = "Получить инсайты", description = "Возвращает рекомендации и коучинг по плану")
    public ResponseEntity<KanbanInsightsResponseDTO> getInsights(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication
    ) {
        log.info("Запрос на инсайты канбана от {}", authentication.getName());
        return ResponseEntity.ok(kanbanService.getInsights(authentication.getName(), startDate, endDate));
    }

    @GetMapping("/optimize/what-if")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    @Operation(summary = "What-if симуляция", description = "Считает несколько сценариев и выбирает лучший")
    public ResponseEntity<KanbanWhatIfResponseDTO> whatIf(
            @RequestParam(defaultValue = "2") Integer minDailyLimit,
            @RequestParam(defaultValue = "5") Integer maxDailyLimit,
            @RequestParam(defaultValue = "1") Integer minBuffer,
            @RequestParam(defaultValue = "3") Integer maxBuffer,
            Authentication authentication
    ) {
        log.info("Запрос на what-if симуляцию канбана от {}", authentication.getName());
        return ResponseEntity.ok(
                kanbanService.whatIf(authentication.getName(), minDailyLimit, maxDailyLimit, minBuffer, maxBuffer)
        );
    }

    @GetMapping("/coach/weekly-plan")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    @Operation(summary = "Недельный план коучинга", description = "Возвращает персональные действия по дням недели")
    public ResponseEntity<KanbanWeeklyPlanResponseDTO> weeklyPlan(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication authentication
    ) {
        log.info("Запрос на недельный план коучинга от {}", authentication.getName());
        return ResponseEntity.ok(kanbanService.getWeeklyPlan(authentication.getName(), weekStart));
    }
}
