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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import ru.danon.spring.ToDo.dto.MyTaskDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.dto.StatusDTO;
import ru.danon.spring.ToDo.dto.TagDTO;
import ru.danon.spring.ToDo.dto.TaskDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.dto.TaskStatDTO;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.services.TagService;
import ru.danon.spring.ToDo.services.TaskService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/task")
@Tag(name = "Task Controller", description = "Управление задачами (создание, назначение, выполнение)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class TaskController {

    private final TaskService taskServiceImpl;
    private final TagService tagServiceImpl;
    private final ModelMapper modelMapper;

    @PostMapping()
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Создать задачу", description = "Создает новую задачу (только для TEACHER)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача успешно создана",
                    content = @Content(schema = @Schema(implementation = TaskResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль TEACHER")
    })
    public TaskResponseDTO createTask(
            @Parameter(description = "Данные задачи", required = true)
            @RequestBody MyTaskDTO taskDTO,
            Authentication authentication) {
        log.info("Запрос на создание задачи от преподавателя: {}", authentication.getName());
        TaskResponseDTO createdTask = taskServiceImpl.createTask(taskDTO, authentication.getName());
        log.info("Задача успешно создана: id={}, title={}", createdTask.getId(), createdTask.getTitle());
        return createdTask;
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Удалить задачу", description = "Удаляет задачу по ID (только для TEACHER)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача успешно удалена"),
            @ApiResponse(responseCode = "404", description = "Задача не найдена"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    public void deleteTask(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId) {
        log.info("Запрос на удаление задачи id={}", taskId);
        taskServiceImpl.deleteTask(taskId);
        log.info("Задача id={} успешно удалена", taskId);
    }

    @GetMapping()
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Получить все задачи", description = "Возвращает страницу со всеми задачами (только для TEACHER)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка задач",
                    content = @Content(schema = @Schema(implementation = TaskDTO.class)))
    })
    public ResponseEntity<Page<TaskDTO>> getTasks(
            @Parameter(description = "Параметры пагинации")
            @PageableDefault Pageable pageable) {
        log.info("Запрос на получение всех задач с пагинацией: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        Page<Task> tasks = taskServiceImpl.findAllTasks(pageable);
        if (tasks.isEmpty()) {
            log.debug("Задачи не найдены");
            return ResponseEntity.ok(Page.empty(pageable));
        }
        List<Integer> taskIds = tasks.getContent().stream()
                .map(Task::getId)
                .collect(Collectors.toList());
        Map<Integer, List<ru.danon.spring.ToDo.models.postgre.Tag>> tagsByTask = tagServiceImpl.getTaskTagsBatch(taskIds);
        log.debug("Получено {} задач из {} всего", tasks.getNumberOfElements(), tasks.getTotalElements());
        return ResponseEntity.ok(tasks
                .map(task -> convertToDTO(task, tagsByTask.get(task.getId()))));
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Получить задачу по ID", description = "Возвращает детальную информацию о задаче")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение задачи",
                    content = @Content(schema = @Schema(implementation = TaskDTO.class))),
            @ApiResponse(responseCode = "404", description = "Задача не найдена")
    })
    public ResponseEntity<TaskDTO> getTask(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId) {
        log.info("Запрос на получение задачи id={}", taskId);
        TaskDTO task = convertToTaskDTO(taskServiceImpl.findTaskById(taskId));
        log.debug("Задача id={} успешно получена", taskId);
        return ResponseEntity.ok(task);
    }

    @GetMapping("/student/{userId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Получить задачи студента", description = "Возвращает задачи, назначенные конкретному студенту")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение задач студента",
                    content = @Content(schema = @Schema(implementation = MyTaskDTO.class)))
    })
    public ResponseEntity<Page<MyTaskDTO>> getTasksStudent(
            @Parameter(description = "Параметры пагинации")
            @PageableDefault Pageable pageable,
            @Parameter(description = "ID студента", required = true)
            @PathVariable Integer userId) {
        log.info("Запрос на получение задач студента id={}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());
        Page<MyTaskDTO> tasks = taskServiceImpl.findUserTasks(userId, pageable);
        log.debug("Получено {} задач для студента id={}", tasks.getNumberOfElements(), userId);
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/assign/{taskID}/{userId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Назначить задачу студенту", description = "Назначает задачу конкретному студенту")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача успешно назначена"),
            @ApiResponse(responseCode = "404", description = "Задача или студент не найдены")
    })
    public ResponseEntity<?> assignTask(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskID,
            @Parameter(description = "ID студента", required = true)
            @PathVariable Integer userId,
            Authentication authentication) {
        log.info("Запрос на назначение задачи id={} студенту id={} от преподавателя: {}",
                taskID, userId, authentication.getName());
        taskServiceImpl.assignTask(taskID, userId, authentication.getName());
        log.info("Задача id={} успешно назначена студенту id={}", taskID, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/assign/{taskID}/group/{groupId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Назначить задачу группе", description = "Назначает задачу всем студентам группы")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача успешно назначена группе"),
            @ApiResponse(responseCode = "404", description = "Задача или группа не найдены")
    })
    public ResponseEntity<Void> assignTaskForGroup(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskID,
            @Parameter(description = "ID группы", required = true)
            @PathVariable Integer groupId,
            Authentication authentication) {
        log.info("Запрос на назначение задачи id={} группе id={} от преподавателя: {}",
                taskID, groupId, authentication.getName());
        taskServiceImpl.assignTaskForGroup(taskID, groupId, authentication.getName());
        log.info("Задача id={} успешно назначена группе id={}", taskID, groupId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/{taskId}/status")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Получить статус выполнения задачи", description = "Возвращает статистику выполнения задачи для группы или студента")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение статуса",
                    content = @Content(schema = @Schema(implementation = TaskStatDTO.class)))
    })
    public ResponseEntity<TaskStatDTO> getStatusTask(
            @Parameter(description = "ID пользователя или группы", required = true)
            @PathVariable Integer id,
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            @Parameter(description = "Тип фильтрации: group или student", required = true, example = "student")
            @RequestParam String filter) {
        log.info("Запрос на получение статуса задачи id={} для {} id={}", taskId, filter, id);
        TaskStatDTO status = taskServiceImpl.findStatusTask(id, taskId, filter);
        log.debug("Статус задачи id={} для {} id={} успешно получен", taskId, filter, id);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Получить мои задачи", description = "Возвращает задачи, назначенные текущему студенту")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение задач",
                    content = @Content(schema = @Schema(implementation = MyTaskDTO.class)))
    })
    public ResponseEntity<Page<MyTaskDTO>> getTasksStudent(
            @Parameter(description = "Параметры пагинации")
            @PageableDefault Pageable pageable,
            Authentication authentication) {
        log.info("Запрос на получение задач студента: {}, page={}, size={}",
                authentication.getName(), pageable.getPageNumber(), pageable.getPageSize());
        Page<MyTaskDTO> tasks = taskServiceImpl.findMyTasks(authentication.getName(), pageable);
        log.debug("Получено {} задач для студента {}", tasks.getNumberOfElements(), authentication.getName());
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/my/{taskId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Получить мою задачу по ID", description = "Возвращает детальную информацию о задаче для студента")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение задачи",
                    content = @Content(schema = @Schema(implementation = MyTaskDTO.class))),
            @ApiResponse(responseCode = "404", description = "Задача не найдена")
    })
    public ResponseEntity<MyTaskDTO> getMyTask(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            Authentication authentication) {
        log.info("Запрос на получение задачи id={} студентом: {}", taskId, authentication.getName());
        MyTaskDTO task = taskServiceImpl.findMyTasksById(taskId, authentication.getName());
        log.debug("Задача id={} успешно получена студентом {}", taskId, authentication.getName());
        return ResponseEntity.ok(task);
    }

    @GetMapping("/my/{taskId}/status")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Получить статус моей задачи", description = "Возвращает статус выполнения задачи для студента")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение статуса",
                    content = @Content(schema = @Schema(implementation = StatusDTO.class)))
    })
    public ResponseEntity<StatusDTO> getTask(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            Authentication authentication) {
        log.info("Запрос на получение статуса задачи id={} студентом: {}", taskId, authentication.getName());
        StatusDTO status = taskServiceImpl.findStatusMyTask(taskId, authentication.getName());
        log.debug("Статус задачи id={} успешно получен студентом {}", taskId, authentication.getName());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/my/{taskId}/status")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Изменить статус моей задачи", description = "Обновляет статус выполнения задачи для студента")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статус успешно обновлен",
                    content = @Content(schema = @Schema(implementation = MyTaskDTO.class)))
    })
    public ResponseEntity<MyTaskDTO> changeStatusMyTask(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            @Parameter(description = "Новый статус", required = true)
            @RequestBody StatusDTO statusDTO,
            Authentication authentication) {
        log.info("Запрос на изменение статуса задачи id={} на {} студентом: {}",
                taskId, statusDTO.getUserStatus(), authentication.getName());
        MyTaskDTO updatedTask = taskServiceImpl.changeMyTask(taskId, statusDTO.getUserStatus(), authentication.getName());
        log.info("Статус задачи id={} успешно изменен на {} студентом {}",
                taskId, statusDTO.getUserStatus(), authentication.getName());
        return ResponseEntity.ok(updatedTask);
    }

    @PostMapping("/my/{taskId}/share/{userId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Поделиться задачей", description = "Позволяет поделиться задачей с другим студентом")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача успешно передана"),
            @ApiResponse(responseCode = "404", description = "Задача или пользователь не найдены")
    })
    public ResponseEntity<Void> shareTask(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable Integer userId,
            Authentication authentication) {
        log.info("Запрос на передачу задачи id={} пользователю id={} от студента: {}",
                taskId, userId, authentication.getName());
        taskServiceImpl.shareTask(taskId, userId, authentication.getName());
        log.info("Задача id={} успешно передана пользователю id={} от студента {}",
                taskId, userId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/getListTask/{taskId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
    @Operation(summary = "Получить пользователей с задачей", description = "Возвращает список пользователей, у которых есть эта задача")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка",
                    content = @Content(schema = @Schema(implementation = PersonResponseDTO.class)))
    })
    public ResponseEntity<List<PersonResponseDTO>> getListTask(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            Authentication authentication) {
        log.info("Запрос на получение пользователей с задачей id={} от: {}", taskId, authentication.getName());
        List<PersonResponseDTO> users = taskServiceImpl.getUsersWithTask(taskId, authentication);
        log.debug("Получено {} пользователей с задачей id={}", users.size(), taskId);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Обновить задачу", description = "Обновляет информацию о задаче")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Задача успешно обновлена",
                    content = @Content(schema = @Schema(implementation = TaskDTO.class))),
            @ApiResponse(responseCode = "404", description = "Задача не найдена")
    })
    public ResponseEntity<TaskDTO> updateTask(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            @Parameter(description = "Обновленные данные задачи", required = true)
            @RequestBody TaskDTO taskDTO,
            Authentication auth) {
        log.info("Запрос на обновление задачи id={} от преподавателя: {}", taskId, auth.getName());
        TaskDTO updatedTask = convertToDTO(taskServiceImpl.updateTask(taskId, taskDTO, auth.getName()));
        log.info("Задача id={} успешно обновлена преподавателем {}", taskId, auth.getName());
        return ResponseEntity.ok(updatedTask);
    }

    private TaskDTO convertToTaskDTO(Task task) {
        return convertToTaskDTO(task, tagServiceImpl.getTaskTags(task.getId()));
    }

    private TaskDTO convertToTaskDTO(Task task, List<ru.danon.spring.ToDo.models.postgre.Tag> tags) {
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
        return convertToDTO(task, tagServiceImpl.getTaskTags(task.getId()));
    }

    private TaskDTO convertToDTO(Task task, List<ru.danon.spring.ToDo.models.postgre.Tag> tags) {
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

    private List<TagDTO> mapTags(List<ru.danon.spring.ToDo.models.postgre.Tag> tags) {
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