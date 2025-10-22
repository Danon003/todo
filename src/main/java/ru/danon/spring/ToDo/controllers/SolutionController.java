package ru.danon.spring.ToDo.controllers;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.danon.spring.ToDo.dto.GradeRequest;
import ru.danon.spring.ToDo.dto.SolutionDTO;
import ru.danon.spring.ToDo.dto.TaskDTO;
import ru.danon.spring.ToDo.services.TaskService;

import java.util.List;

@RestController
@RequestMapping("/minio/tasks/{taskId}/solution")
@RequiredArgsConstructor
public class SolutionController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<Void> uploadSolution(
            @PathVariable Integer taskId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        taskService.uploadSolution(taskId, file, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/download")
    public ResponseEntity<String> getSolutionDownloadUrl(
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
    public ResponseEntity<Void> deleteSolution(
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
    public ResponseEntity<List<SolutionDTO>> getAllSolutions(
            @PathVariable Integer taskId,
            Authentication authentication
    ){
        List<SolutionDTO> solutions = taskService.getAllSolutionsForTask(taskId, authentication.getName());
        return ResponseEntity.ok(solutions);
    }

    // Преподаватель оценивает решение
    @PutMapping("/{studentId}/grade")
    public ResponseEntity<Void> gradeSolution(
            @PathVariable Integer taskId,
            @PathVariable Integer studentId,
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

    // Преподаватель скачивает решение студента
    @GetMapping("/{studentId}/download")
    public ResponseEntity<String> getStudentSolutionDownloadUrl(
            @PathVariable Integer taskId,
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
    public ResponseEntity<SolutionDTO> getStudentSolution(@PathVariable Integer taskId, Authentication auth) {
        return ResponseEntity.ok(taskService.getStudentSolution(taskId, auth));
    }

}