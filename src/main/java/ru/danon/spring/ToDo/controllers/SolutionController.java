package ru.danon.spring.ToDo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.danon.spring.ToDo.dto.GradeRequest;
import ru.danon.spring.ToDo.dto.SolutionDTO;
import ru.danon.spring.ToDo.services.TaskService;

import java.util.List;

@RestController
@RequestMapping("/minio/tasks/{taskId}/solution")
@RequiredArgsConstructor
@Tag(name = "Solution Controller", description = "Управление решениями задач (загрузка, скачивание, оценка)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class SolutionController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Загрузить решение", description = "Загружает файл с решением задачи (для студента)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Решение успешно загружено"),
            @ApiResponse(responseCode = "400", description = "Ошибка загрузки файла"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    public ResponseEntity<Void> uploadSolution(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            @Parameter(description = "Файл решения", required = true)
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        log.info("Запрос на загрузку решения к задаче id={} от пользователя: {}, файл: {}",
                taskId, authentication.getName(), file.getOriginalFilename());
        taskService.uploadSolution(taskId, file, authentication.getName());
        log.info("Решение успешно загружено к задаче id={} пользователем {}", taskId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/download")
    @Operation(summary = "Получить ссылку на скачивание решения", description = "Возвращает URL для скачивания решения (для студента)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ссылка успешно получена",
                    content = @Content(schema = @Schema(example = "https://minio.example.com/bucket/solution.pdf"))),
            @ApiResponse(responseCode = "404", description = "Решение не найдено")
    })
    public ResponseEntity<String> getSolutionDownloadUrl(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            Authentication authentication) {

        log.info("Запрос на получение ссылки для скачивания решения к задаче id={} от пользователя: {}",
                taskId, authentication.getName());
        String downloadUrl = taskService.getSolutionDownloadUrl(taskId, authentication.getName());
        log.debug("Ссылка на скачивание решения получена для задачи id={}", taskId);
        return ResponseEntity.ok(downloadUrl);
    }

    @DeleteMapping
    @Operation(summary = "Удалить решение", description = "Удаляет загруженное решение (для студента)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Решение успешно удалено"),
            @ApiResponse(responseCode = "404", description = "Решение не найдено")
    })
    public ResponseEntity<Void> deleteSolution(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            Authentication authentication) {

        log.info("Запрос на удаление решения к задаче id={} от пользователя: {}", taskId, authentication.getName());
        taskService.deleteSolution(taskId, authentication.getName());
        log.info("Решение к задаче id={} успешно удалено пользователем {}", taskId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "Получить все решения задачи", description = "Возвращает список всех решений для задачи (только для TEACHER)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка решений",
                    content = @Content(schema = @Schema(implementation = SolutionDTO.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль TEACHER")
    })
    public ResponseEntity<List<SolutionDTO>> getAllSolutions(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            Authentication authentication) {
        log.info("Запрос на получение всех решений к задаче id={} от преподавателя: {}",
                taskId, authentication.getName());
        List<SolutionDTO> solutions = taskService.getAllSolutionsForTask(taskId, authentication.getName());
        log.debug("Получено {} решений для задачи id={}", solutions.size(), taskId);
        return ResponseEntity.ok(solutions);
    }

    @PutMapping("/{studentId}/grade")
    @Operation(summary = "Оценить решение", description = "Преподаватель оценивает решение студента")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Оценка успешно выставлена"),
            @ApiResponse(responseCode = "400", description = "Ошибка при выставлении оценки"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    public ResponseEntity<Void> gradeSolution(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            @Parameter(description = "ID студента", required = true)
            @PathVariable Integer studentId,
            @Parameter(description = "Данные оценки", required = true)
            @RequestBody GradeRequest gradeRequest,
            Authentication authentication) {
        log.info("Запрос на оценку решения: задача id={}, студент id={}, оценка={}, от преподавателя: {}",
                taskId, studentId, gradeRequest.getGrade(), authentication.getName());
        taskService.gradeSolution(
                taskId,
                studentId,
                gradeRequest.getGrade(),
                gradeRequest.getComment(),
                authentication.getName()
        );
        log.info("Оценка успешно выставлена: задача id={}, студент id={}, оценка={}",
                taskId, studentId, gradeRequest.getGrade());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{studentId}/download")
    @Operation(summary = "Скачать решение студента", description = "Преподаватель скачивает решение студента")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ссылка на скачивание получена"),
            @ApiResponse(responseCode = "404", description = "Решение не найдено")
    })
    public ResponseEntity<String> getStudentSolutionDownloadUrl(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            @Parameter(description = "ID студента", required = true)
            @PathVariable Integer studentId,
            Authentication authentication) {

        log.info("Запрос на скачивание решения студента id={} к задаче id={} от преподавателя: {}",
                studentId, taskId, authentication.getName());
        String downloadUrl = taskService.getStudentSolutionDownloadUrl(taskId, studentId, authentication.getName());
        log.debug("Ссылка на скачивание решения получена: задача id={}, студент id={}", taskId, studentId);
        return ResponseEntity.ok(downloadUrl);
    }

    @GetMapping
    @Operation(summary = "Получить решение студента", description = "Возвращает информацию о решении текущего студента")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение информации о решении",
                    content = @Content(schema = @Schema(implementation = SolutionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Решение не найдено")
    })
    public ResponseEntity<SolutionDTO> getStudentSolution(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            Authentication auth) {
        log.info("Запрос на получение информации о решении к задаче id={} от пользователя: {}",
                taskId, auth.getName());
        SolutionDTO solution = taskService.getStudentSolution(taskId, auth);
        log.debug("Информация о решении получена для задачи id={}", taskId);
        return ResponseEntity.ok(solution);
    }
}