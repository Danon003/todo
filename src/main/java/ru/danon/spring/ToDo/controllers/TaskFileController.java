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
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.danon.spring.ToDo.dto.TaskFileDTO;
import ru.danon.spring.ToDo.mappers.TaskFileMapper;
import ru.danon.spring.ToDo.models.postgre.TaskFile;
import ru.danon.spring.ToDo.services.TaskFileService;

import java.util.List;

@RestController
@RequestMapping("/minio/tasks/{taskId}/files")
@RequiredArgsConstructor
@Tag(name = "Task File Controller", description = "Управление файлами, прикрепленными к задачам")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class TaskFileController {

    private final TaskFileService taskFileService;
    private final TaskFileMapper taskFileMapper;

    @PostMapping
    @Operation(summary = "Загрузить файл к задаче", description = "Прикрепляет файл к задаче")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Файл успешно загружен",
                    content = @Content(schema = @Schema(implementation = TaskFileDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка загрузки файла"),
            @ApiResponse(responseCode = "404", description = "Задача не найдена")
    })
    public ResponseEntity<TaskFileDTO> uploadTaskFile(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            @Parameter(description = "Файл для загрузки", required = true)
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        log.info("Запрос на загрузку файла к задаче id={} от пользователя: {}, файл: {}",
                taskId, authentication.getName(), file.getOriginalFilename());
        TaskFileDTO taskFile = taskFileMapper.toDTO(taskFileService.uploadTaskFile(taskId, file, authentication));
        log.info("Файл {} успешно загружен к задаче id={}, fileId={}",
                file.getOriginalFilename(), taskId, taskFile.getId());
        return ResponseEntity.ok(taskFile);
    }

    @GetMapping
    @Operation(summary = "Получить файлы задачи", description = "Возвращает список файлов, прикрепленных к задаче")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка файлов",
                    content = @Content(schema = @Schema(implementation = TaskFileDTO.class)))
    })
    public ResponseEntity<List<TaskFileDTO>> getTaskFiles(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId) {
        log.info("Запрос на получение файлов задачи id={}", taskId);
        List<TaskFileDTO> files = taskFileService.getTaskFiles(taskId)
                .stream()
                .map(taskFileMapper::toDTO)
                .toList();
        log.debug("Получено {} файлов для задачи id={}", files.size(), taskId);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Удалить файл задачи", description = "Удаляет прикрепленный файл")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Файл успешно удален"),
            @ApiResponse(responseCode = "404", description = "Файл не найден")
    })
    public ResponseEntity<Void> deleteTaskFile(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            @Parameter(description = "ID файла", required = true)
            @PathVariable Integer fileId) {
        log.info("Запрос на удаление файла id={} из задачи id={}", fileId, taskId);
        taskFileService.deleteTaskFile(fileId);
        log.info("Файл id={} успешно удален из задачи id={}", fileId, taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{fileId}/download")
    @Operation(summary = "Получить ссылку на скачивание файла", description = "Возвращает URL для скачивания файла")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ссылка успешно получена",
                    content = @Content(schema = @Schema(example = "https://minio.example.com/bucket/file.pdf"))),
            @ApiResponse(responseCode = "404", description = "Файл не найден")
    })
    public ResponseEntity<String> getDownloadUrl(
            @Parameter(description = "ID файла", required = true)
            @PathVariable Integer fileId,
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId) {
        log.info("Запрос на получение ссылки для скачивания файла id={} из задачи id={}", fileId, taskId);
        String downloadUrl = taskFileService.getFileDownloadUrl(fileId);
        log.debug("Ссылка на скачивание файла id={} успешно сгенерирована", fileId);
        return ResponseEntity.ok(downloadUrl);
    }
}