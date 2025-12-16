package ru.danon.spring.ToDo.controllers;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.danon.spring.ToDo.dto.*;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.models.Tag;
import ru.danon.spring.ToDo.models.Task;
import ru.danon.spring.ToDo.services.TagService;
import ru.danon.spring.ToDo.services.TaskService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/task")
public class TaskController {

    private final TaskService taskService;
    private final TagService tagService;
    private final ModelMapper modelMapper;

    @Autowired
    public TaskController(TaskService taskService, TagService tagService, ModelMapper modelMapper) {
        this.taskService = taskService;
        this.tagService = tagService;
        this.modelMapper = modelMapper;
    }

    //работает
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping()
    public TaskResponseDTO createTask(@RequestBody MyTaskDTO taskDTO, Authentication authentication) {
        return taskService.createTask(taskDTO, authentication.getName());
    }

    //работает
    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{taskId}")
    public void deleteTask(@PathVariable Integer taskId) {
        taskService.deleteTask(taskId);
    }

    //работает
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping()
    public ResponseEntity<List<TaskDTO>> getTasks() {
        List<Task> tasks = taskService.findAllTasks();
        if (tasks.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<Integer> taskIds = tasks.stream()
                .map(Task::getId)
                .collect(Collectors.toList());
        Map<Integer, List<Tag>> tagsByTask = tagService.getTaskTagsBatch(taskIds);

        return ResponseEntity.ok(tasks.stream()
                .map(task -> convertToDTO(task, tagsByTask.get(task.getId())))
                .collect(Collectors.toList()));
    }

    //работает
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDTO> getTask(@PathVariable Integer taskId) {
        return ResponseEntity.ok(convertToTaskDTO(taskService.findTaskById(taskId)));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/student/{userId}")
    public ResponseEntity<List<MyTaskDTO>> getTasksStudent(@PathVariable Integer userId) {
        List<MyTaskDTO> tasks = taskService.findUserTasks(userId);
        return ResponseEntity.ok(tasks);
    }

    //работает
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/assign/{taskID}/{userId}")
    public ResponseEntity<?> assignTask(@PathVariable Integer taskID,
                                        @PathVariable Integer userId,
                                        Authentication authentication) {
        taskService.assignTask(taskID, userId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    //работает
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/assign/{taskID}/group/{groupId}")
    public ResponseEntity<Void> assignTaskForGroup(@PathVariable Integer taskID,
                                                   @PathVariable Integer groupId,
                                                   Authentication authentication) {
        taskService.assignTaskForGroup(taskID, groupId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    //работает
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{id}/{taskId}/status")
    public ResponseEntity<TaskStatDTO> getStatusTask(@PathVariable Integer taskId,
                                                     @PathVariable Integer id,
                                                     //filter=group||filter=student
                                                     @RequestParam String filter) {
        return ResponseEntity.ok(taskService.findStatusTask(id, taskId, filter));
    }

    //работает
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my")
    public ResponseEntity<List<MyTaskDTO>> getTasksStudent(Authentication authentication) {
        List<MyTaskDTO> tasks = taskService.findMyTasks(authentication.getName());
        return ResponseEntity.ok(tasks);
    }

    //работает
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my/{taskId}")
    public ResponseEntity<MyTaskDTO> getMyTask(@PathVariable Integer taskId, Authentication authentication) {
        return ResponseEntity.ok(taskService.findMyTasksById(taskId, authentication.getName()));
    }


    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my/{taskId}/status")
    public ResponseEntity<StatusDTO> getTask(@PathVariable Integer taskId,
                                             Authentication authentication) {
        return ResponseEntity.ok(taskService.findStatusMyTask(taskId, authentication.getName()));
    }


    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/my/{taskId}/status")
    public ResponseEntity<MyTaskDTO> changeStatusMyTask(@PathVariable Integer taskId,
                                                        @RequestBody StatusDTO statusDTO,
                                                        Authentication authentication) {
        return ResponseEntity.ok(taskService.changeMyTask(taskId, statusDTO.getStatus(), authentication.getName()));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/my/{taskId}/share/{userId}")
    public ResponseEntity<Void> shareTask(@PathVariable Integer taskId,
                                          @PathVariable Integer userId,
                                          Authentication authentication) {
        taskService.shareTask(taskId, userId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    @GetMapping("/getListTask/{taskId}")
    public ResponseEntity<List<PersonResponseDTO>> getListTask(@PathVariable Integer taskId, Authentication authentication) {
        return ResponseEntity.ok(taskService.getUsersWithTask(taskId, authentication));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Integer taskId, @RequestBody TaskDTO taskDTO, Authentication auth) {
        return ResponseEntity.ok(convertToDTO(taskService.updateTask(taskId, taskDTO, auth.getName())));
    }



    private TaskDTO convertToTaskDTO(Task task) {
        return convertToTaskDTO(task, tagService.getTaskTags(task.getId()));
    }

    private TaskDTO convertToTaskDTO(Task task, List<Tag> tags) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setDeadline(task.getDeadline());
        dto.setPriority(task.getPriority());
        dto.setAuthorId(task.getAuthor() != null ? task.getAuthor().getId() : null);

        dto.setTags(mapTags(tags));

        return dto;
    }

    private TaskDTO convertToDTO(Task task) {
        return convertToDTO(task, tagService.getTaskTags(task.getId()));
    }

    private TaskDTO convertToDTO(Task task, List<Tag> tags) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setDeadline(task.getDeadline());
        dto.setPriority(task.getPriority());
        dto.setAuthorId(task.getAuthor() != null ? task.getAuthor().getId() : null);

        dto.setTags(mapTags(tags));

        return dto;
    }

    private List<TagDTO> mapTags(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }

        return tags.stream()
                .filter(Objects::nonNull)
                .map(tag -> new TagDTO(tag.getId(), tag.getName()))
                .collect(Collectors.toList());
    }

    private MyTaskDTO convertToMyTaskDTO(Task task) {
        return modelMapper.map(task, MyTaskDTO.class);
    }
    private StatusDTO convertToStatusDTO(MyTaskDTO statusMyTask) {
        return modelMapper.map(statusMyTask, StatusDTO.class);
    }
    private Task convertToTask(TaskDTO taskDTO) {
        return modelMapper.map(taskDTO, Task.class);
    }
}
