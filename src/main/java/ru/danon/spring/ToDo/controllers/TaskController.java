package ru.danon.spring.ToDo.controllers;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.danon.spring.ToDo.dto.StatusDTO;
import ru.danon.spring.ToDo.dto.TaskDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.dto.TaskStatDTO;
import ru.danon.spring.ToDo.models.Task;
import ru.danon.spring.ToDo.services.TaskService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/task")
public class TaskController {

    private final TaskService taskService;
    private final ModelMapper modelMapper;

    @Autowired
    public TaskController(TaskService taskService, ModelMapper modelMapper) {
        this.taskService = taskService;
        this.modelMapper = modelMapper;
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping()
    public TaskResponseDTO createTask(@RequestBody TaskDTO taskDTO, Authentication authentication) {
        return taskService.createTask(taskDTO, authentication.getName());
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{taskId}")
    public void deleteTask(@PathVariable Integer taskId) {
         taskService.deleteTask(taskId);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping()
    public ResponseEntity<List<TaskDTO>> getTasks() {
        return ResponseEntity.ok(taskService.findAllTasks()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
    }


    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDTO> getTask(@PathVariable Integer taskId) {
        return ResponseEntity.ok(convertToTaskDTO(taskService.findTaskById(taskId)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/assign/{taskID}/{userId}")
    public ResponseEntity<?> assignTask(@PathVariable Integer taskID,
                                                  @PathVariable Integer userId,
                                                     Authentication authentication) {
        taskService.assignTask(taskID, userId, authentication.getName());
       return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/assign/{taskID}/group/{groupId}")
    public ResponseEntity<Void> assignTaskForGroup(@PathVariable Integer taskID,
                                                  @PathVariable Integer groupId,
                                                   Authentication authentication) {
        taskService.assignTaskForGroup(taskID, groupId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{id}/{taskId}/status")
    public ResponseEntity<TaskStatDTO> getStatusTask(@PathVariable Integer taskId,
                                                     @PathVariable Integer id,
                                                     //filter=group||filter=student
                                                     @RequestParam String filter) {
        return ResponseEntity.ok(taskService.findStatusTask(id, taskId, filter));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my")
    public ResponseEntity<List<TaskDTO>> getTasksStudent(Authentication authentication) {
        return ResponseEntity.ok(taskService.findMyTasks(authentication.getName())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my/{taskId}")
    public ResponseEntity<TaskDTO> getTasks(@PathVariable Integer taskId, Authentication authentication) {
        return ResponseEntity.ok(convertToTaskDTO(taskService.findMyTasksById(taskId, authentication.getName())));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my/{taskId}/status")
    public ResponseEntity<StatusDTO> getTask(@PathVariable Integer taskId,
                                             Authentication authentication) {
        return ResponseEntity.ok(convertToStatusDTO(taskService.findStatusMyTask(taskId, authentication.getName())));
    }


    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/my/{taskId}/status")
    public ResponseEntity<TaskDTO> changeStatusMyTask(@PathVariable Integer taskId,
                                            @RequestBody StatusDTO statusDTO,
                                                      Authentication authentication) {
        return ResponseEntity.ok(convertToTaskDTO(
                taskService.changeMyTask(taskId, convertDTOToStatus(statusDTO).getStatus(), authentication.getName())));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/my/{taskId}/share/{userId}")
    public ResponseEntity<Void> shareTask(@PathVariable Integer taskId,
                                             @PathVariable Integer userId,
                                             Authentication authentication) {
        taskService.shareTask(taskId, userId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    private TaskDTO convertToTaskDTO(Task task) {
        return modelMapper.map(task, TaskDTO.class);
    }

    private TaskDTO convertToDTO(Task task) {
        return modelMapper.map(task, TaskDTO.class);
    }

    private Task convertDTOToStatus(StatusDTO statusDTO) {
        return modelMapper.map(statusDTO, Task.class);
    }

    private StatusDTO convertToStatusDTO(Task statusMyTask) {
        return modelMapper.map(statusMyTask, StatusDTO.class);
    }
}
