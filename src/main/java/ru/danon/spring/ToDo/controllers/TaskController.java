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
import ru.danon.spring.ToDo.dto.BulkAssignRequestDTO;
import ru.danon.spring.ToDo.dto.TaskPriorityDTO;
import ru.danon.spring.ToDo.dto.TaskDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.dto.TaskStatDTO;
import ru.danon.spring.ToDo.mappers.TaskMapper;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.services.TagService;
import ru.danon.spring.ToDo.services.TaskService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/task")
@Tag(name = "Task Controller", description = "Управление задачами (создание, назначение, выполнение)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class TaskController {

    private final TaskService taskService;
    private final TagService tagService;
    private final TaskMapper taskMapper;

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
        TaskResponseDTO createdTask = taskService.createTask(taskDTO, authentication.getName());
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
    public ResponseEntity<Void> deleteTask(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Long taskId) {
        log.info("Запрос на удаление задачи id={}", taskId);
        taskService.deleteTask(taskId);
        log.info("Задача id={} успешно удалена", taskId);
        return ResponseEntity.noContent().build();
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
        log.info("Запрос на получение всех задач: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Task> tasks = taskService.findAllTasks(pageable);
        if (tasks.isEmpty()) {
            return ResponseEntity.ok(Page.empty(pageable));
        }

        List<Long> taskIds = tasks.getContent().stream().map(Task::getId).toList();
        Map<Long, List<ru.danon.spring.ToDo.models.postgre.Tag>> tagsByTask = tagService.getTaskTagsBatch(taskIds);

        Page<TaskDTO> dtoPage = taskMapper.toDtoPage(tasks, tagsByTask);
        log.debug("Получено {} задач из {} всего", dtoPage.getNumberOfElements(), dtoPage.getTotalElements());
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Получить все активные задачи", description = "Возвращает страницу со всеми активными задачами (только для TEACHER)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка задач",
                    content = @Content(schema = @Schema(implementation = TaskDTO.class)))
    })
    public ResponseEntity<Page<TaskDTO>> getActiveTasks(
            @Parameter(description = "Параметры пагинации")
            @PageableDefault Pageable pageable) {
        log.info("Запрос на получение всех задач: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Task> tasks = taskService.findAllActiveTasks(pageable);
        if (tasks.isEmpty()) {
            return ResponseEntity.ok(Page.empty(pageable));
        }

        List<Long> taskIds = tasks.getContent().stream().map(Task::getId).toList();
        Map<Long, List<ru.danon.spring.ToDo.models.postgre.Tag>> tagsByTask = tagService.getTaskTagsBatch(taskIds);

        Page<TaskDTO> dtoPage = taskMapper.toDtoPage(tasks, tagsByTask);
        log.debug("Получено {} задач из {} всего", dtoPage.getNumberOfElements(), dtoPage.getTotalElements());
        return ResponseEntity.ok(dtoPage);
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
            @PathVariable Long taskId) {
        log.info("Запрос на получение задачи id={}", taskId);
        TaskDTO task = taskMapper.toDto(taskService.findTaskById(taskId));
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
            @PathVariable Long userId) {
        log.info("Запрос на получение задач студента id={}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());
        Page<MyTaskDTO> tasks = taskService.findUserTasks(userId, pageable);
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
    public ResponseEntity<Void> assignTask(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Long taskID,
            @Parameter(description = "ID студента", required = true)
            @PathVariable Long userId,
            Authentication authentication) {
        log.info("Запрос на назначение задачи id={} студенту id={} от преподавателя: {}",
                taskID, userId, authentication.getName());
        taskService.assignTask(taskID, userId, authentication.getName());
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
            @PathVariable Long taskID,
            @Parameter(description = "ID группы", required = true)
            @PathVariable Long groupId,
            Authentication authentication) {
        log.info("Запрос на назначение задачи id={} группе id={} от преподавателя: {}",
                taskID, groupId, authentication.getName());
        taskService.assignTaskForGroup(taskID, groupId, authentication.getName());
        log.info("Задача id={} успешно назначена группе id={}", taskID, groupId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/assign/groups")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Массово назначить задачи группам", description = "Назначает список задач списку групп")
    public ResponseEntity<Map<String, Integer>> assignTasksForGroups(
            @RequestBody BulkAssignRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(taskService.assignTasksForGroups(
                request.getTaskIds(),
                request.getGroupIds(),
                authentication.getName()
        ));
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
            @PathVariable Long id,
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Long taskId,
            @Parameter(description = "Тип фильтрации: group или student", required = true, example = "student")
            @RequestParam String filter) {
        log.info("Запрос на получение статуса задачи id={} для {} id={}", taskId, filter, id);
        TaskStatDTO status = taskService.findStatusTask(id, taskId, filter);
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
        Page<MyTaskDTO> tasks = taskService.findMyTasks(authentication.getName(), pageable);
        log.debug("Получено {} задач для студента {}", tasks.getNumberOfElements(), authentication.getName());
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/my/active")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Получить мои активные задачи", description = "Возвращает задачи без просроченных с корректной пагинацией")
    public ResponseEntity<Page<MyTaskDTO>> getMyActiveTasks(
            @PageableDefault Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(taskService.findMyActiveTasks(authentication.getName(), pageable));
    }

    @DeleteMapping("/my/overdue")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Удалить мои просроченные назначения", description = "Удаляет просроченные назначения текущего студента")
    public ResponseEntity<Map<String, Integer>> deleteMyOverdueAssignments(Authentication authentication) {
        int removed = taskService.deleteMyOverdueAssignments(authentication.getName());
        return ResponseEntity.ok(Map.of("removed", removed));
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
            @PathVariable Long taskId,
            Authentication authentication) {
        log.info("Запрос на получение задачи id={} студентом: {}", taskId, authentication.getName());
        MyTaskDTO task = taskService.findMyTasksById(taskId, authentication.getName());
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
            @PathVariable Long taskId,
            Authentication authentication) {
        log.info("Запрос на получение статуса задачи id={} студентом: {}", taskId, authentication.getName());
        StatusDTO status = taskService.findStatusMyTask(taskId, authentication.getName());
        log.debug("Статус задачи id={} успешно получен студентом {}", taskId, authentication.getName());
        return ResponseEntity.ok(status);
    }

    @PutMapping("/my/{taskId}/priority")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Изменить приоритет моей задачи", description = "Обновляет приоритет назначения для текущего студента")
    public ResponseEntity<MyTaskDTO> changeMyTaskPriority(
            @PathVariable Long taskId,
            @RequestBody TaskPriorityDTO priorityDTO,
            Authentication authentication
    ) {
        return ResponseEntity.ok(taskService.updateMyTaskPriority(taskId, priorityDTO, authentication.getName()));
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
            @PathVariable Long taskId,
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable Long userId,
            Authentication authentication) {
        log.info("Запрос на передачу задачи id={} пользователю id={} от студента: {}",
                taskId, userId, authentication.getName());
        taskService.shareTask(taskId, userId, authentication.getName());
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
            @PathVariable Long taskId,
            Authentication authentication) {
        log.info("Запрос на получение пользователей с задачей id={} от: {}", taskId, authentication.getName());
        List<PersonResponseDTO> users = taskService.getUsersWithTask(taskId, authentication);
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
            @PathVariable Long taskId,
            @Parameter(description = "Обновленные данные задачи", required = true)
            @RequestBody TaskDTO taskDTO,
            Authentication auth) {
        log.info("Запрос на обновление задачи id={} от преподавателя: {}", taskId, auth.getName());
        TaskDTO updatedTask = taskMapper.toDto(taskService.updateTask(taskId, taskDTO, auth.getName()));
        log.info("Задача id={} успешно обновлена преподавателем {}", taskId, auth.getName());
        return ResponseEntity.ok(updatedTask);
    }
}