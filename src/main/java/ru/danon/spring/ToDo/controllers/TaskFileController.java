package ru.danon.spring.ToDo.controllers;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.danon.spring.ToDo.dto.TaskFileDTO;
import ru.danon.spring.ToDo.models.TaskFile;
import ru.danon.spring.ToDo.services.TaskFileService;

import java.util.List;

@RestController
@RequestMapping("/minio/tasks/{taskId}/files")
@RequiredArgsConstructor
public class TaskFileController {

    private final TaskFileService taskFileService;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<TaskFileDTO> uploadTaskFile(
            @PathVariable Integer taskId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        TaskFileDTO taskFile = convertToTaskFileDTO(taskFileService.uploadTaskFile(taskId, file, authentication));
        return ResponseEntity.ok(taskFile);
    }

    @GetMapping
    public ResponseEntity<List<TaskFileDTO>> getTaskFiles(@PathVariable Integer taskId) {
        return ResponseEntity.ok(taskFileService.getTaskFiles(taskId)
                .stream()
                .map(this::convertToTaskFileDTO)
                .toList());
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteTaskFile(
            @PathVariable Integer taskId,
            @PathVariable Integer fileId) {

        taskFileService.deleteTaskFile(fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<String> getDownloadUrl(@PathVariable Integer fileId, @PathVariable Integer taskId) {
        String downloadUrl = taskFileService.getFileDownloadUrl(fileId);
        return ResponseEntity.ok(downloadUrl);
    }

    private TaskFileDTO convertToTaskFileDTO(TaskFile taskFile) {
        return modelMapper.map(taskFile, TaskFileDTO.class);
    }
}