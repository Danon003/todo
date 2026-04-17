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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.danon.spring.ToDo.dto.GroupResponseDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.mappers.GroupMapper;
import ru.danon.spring.ToDo.services.AdminService;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.TaskService;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group")
@Tag(name = "Group Controller", description = "Управление группами студентов")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class GroupController {

    private final GroupService groupService;
    private final AdminService adminService;
    private final TaskService taskService;
    private final GroupMapper groupMapper;

    @GetMapping()
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    @Operation(summary = "Получить все группы", description = "Возвращает страницу со списком групп (доступно для всех авторизованных)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка групп",
                    content = @Content(schema = @Schema(implementation = GroupResponseDTO.class)))
    })
    public Page<GroupResponseDTO> getAllGroups(
            @Parameter(description = "Параметры пагинации (size, page, sort)")
            @PageableDefault(size = 10) Pageable pageable,
            Authentication auth) {
        log.info("Запрос на получение всех групп от пользователя: {}, page={}, size={}", auth.getName(), pageable.getPageNumber(), pageable.getPageSize());
        Page<GroupResponseDTO> groups = groupService.findAll(auth, pageable);
        log.debug("Получено {} групп из {} всего", groups.getNumberOfElements(), groups.getTotalElements());
        return groups;
    }

    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать группу", description = "Создает новую группу (только ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Группа успешно создана"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль ADMIN")
    })
    public ResponseEntity<GroupResponseDTO> createGroup(
            @Parameter(description = "Название группы", required = true)
            @RequestParam String name,
            @Parameter(description = "Описание группы")
            @RequestParam(required = false) String description) {
        log.info("Запрос на создание группы: name={}, description={}", name, description);
        adminService.createGroup(name, description);
        log.info("Группа успешно создана: {}", name);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{groupId}/students")
    @Operation(summary = "Получить студентов группы", description = "Возвращает список студентов указанной группы")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка студентов",
                    content = @Content(schema = @Schema(implementation = PersonResponseDTO.class)))
    })
    public ResponseEntity<List<PersonResponseDTO>> studentsGroup(
            @Parameter(description = "ID группы", required = true)
            @PathVariable Integer groupId) {
        log.info("Запрос на получение студентов группы id={}", groupId);
        List<PersonResponseDTO> students = groupService.getStudentsByGroupId(groupId);
        log.debug("Получено {} студентов в группе id={}", students.size(), groupId);
        return ResponseEntity.ok(students);
    }

    @PostMapping("/{groupId}/students/{studentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Добавить студента в группу", description = "Добавляет студента в указанную группу (ADMIN или TEACHER)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Студент успешно добавлен в группу"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Группа или студент не найдены")
    })
    public ResponseEntity<Void> addStudentToGroup(
            @Parameter(description = "ID группы", required = true)
            @PathVariable Integer groupId,
            @Parameter(description = "ID студента", required = true)
            @PathVariable Integer studentId) {
        log.info("Запрос на добавление студента id={} в группу id={}", studentId, groupId);
        groupService.addStudentToGroup(groupId, studentId);
        log.info("Студент id={} успешно добавлен в группу id={}", studentId, groupId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/students/{studentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Удалить студента из группы", description = "Удаляет студента из указанной группы (ADMIN или TEACHER)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Студент успешно удален из группы"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    public ResponseEntity<Void> deleteStudentFromGroup(
            @Parameter(description = "ID группы", required = true)
            @PathVariable Integer groupId,
            @Parameter(description = "ID студента", required = true)
            @PathVariable Integer studentId) {
        log.info("Запрос на удаление студента id={} из группы id={}", studentId, groupId);
        groupService.removeStudentFromGroup(groupId, studentId);
        log.info("Студент id={} успешно удален из группы id={}", studentId, groupId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить группу", description = "Удаляет группу (только ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Группа успешно удалена"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль ADMIN")
    })
    public ResponseEntity<Void> deleteGroup(
            @Parameter(description = "ID группы", required = true)
            @PathVariable Integer groupId) {
        log.info("Запрос на удаление группы id={}", groupId);
        groupService.removeGroup(groupId);
        log.info("Группа id={} успешно удалена", groupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    @Operation(summary = "Получить информацию о группе", description = "Возвращает детальную информацию о группе")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение информации о группе",
                    content = @Content(schema = @Schema(implementation = GroupResponseDTO.class)))
    })
    public ResponseEntity<GroupResponseDTO> getGroup(
            @Parameter(description = "ID группы", required = true)
            @PathVariable Integer groupId,
            Authentication auth) {
        log.info("Запрос на получение информации о группе id={} от пользователя: {}", groupId, auth.getName());
        GroupResponseDTO group = groupService.findById(groupId, auth);
        log.debug("Информация о группе id={} успешно получена", groupId);
        return ResponseEntity.ok(group);
    }

    @GetMapping("/{groupId}/tasks")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Получить задачи группы", description = "Возвращает список задач, назначенных на группу")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка задач",
                    content = @Content(schema = @Schema(implementation = TaskResponseDTO.class)))
    })
    public ResponseEntity<Set<TaskResponseDTO>> getGroupTasks(
            @Parameter(description = "ID группы", required = true)
            @PathVariable Integer groupId) {
        log.info("Запрос на получение задач группы id={}", groupId);
        Set<TaskResponseDTO> tasks = taskService.getGroupTasks(groupId);
        log.debug("Получено {} задач для группы id={}", tasks.size(), groupId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/my-students")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Получить моих студентов", description = "Возвращает список студентов, привязанных к преподавателю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка студентов",
                    content = @Content(schema = @Schema(implementation = PersonResponseDTO.class)))
    })
    public ResponseEntity<List<PersonResponseDTO>> getStudents(Authentication auth) {
        log.info("Запрос на получение студентов преподавателя: {}", auth.getName());
        List<PersonResponseDTO> students = groupMapper.toDTOList(groupService.findByTeacherId(auth));
        log.debug("Получено {} студентов для преподавателя {}", students.size(), auth.getName());
        return ResponseEntity.ok(students);
    }

    @GetMapping("/students_has_group")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Получить студентов, имеющих группу", description = "Возвращает список студентов, которые уже состоят в группах")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка студентов",
                    content = @Content(schema = @Schema(implementation = PersonResponseDTO.class)))
    })
    public ResponseEntity<List<PersonResponseDTO>> getStudentsHasGroup() {
        log.info("Запрос на получение студентов, имеющих группу");
        List<PersonResponseDTO> students = groupService.getStudentsHasGroup();
        log.debug("Получено {} студентов, имеющих группу", students.size());
        return ResponseEntity.ok(students);
    }


}