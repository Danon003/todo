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
        taskService.uploadSolution(taskId, file, authentication.getName());
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
        try {
            String downloadUrl = taskService.getSolutionDownloadUrl(taskId, authentication.getName());
            return ResponseEntity.ok(downloadUrl);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
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
        try {
            taskService.deleteSolution(taskId, authentication.getName());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
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
        List<SolutionDTO> solutions = taskService.getAllSolutionsForTask(taskId, authentication.getName());
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
        taskService.gradeSolution(
                taskId,
                studentId,
                gradeRequest.getGrade(),
                gradeRequest.getComment(),
                authentication.getName()
        );
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
        try {
            String downloadUrl = taskService.getStudentSolutionDownloadUrl(taskId, studentId, authentication.getName());
            return ResponseEntity.ok(downloadUrl);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
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
        return ResponseEntity.ok(taskService.getStudentSolution(taskId, auth));
    }
}