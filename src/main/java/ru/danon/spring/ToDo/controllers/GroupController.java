package ru.danon.spring.ToDo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.danon.spring.ToDo.dto.GroupResponseDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.models.Group;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.services.AdminService;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.TaskService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/group")
@Tag(name = "Group Controller", description = "Управление группами студентов")
@SecurityRequirement(name = "bearerAuth")
public class GroupController {

    private final GroupService groupService;
    private final AdminService adminService;
    private final TaskService taskService;
    private final ModelMapper modelMapper;

    @Autowired
    public GroupController(GroupService groupService, AdminService adminService, TaskService taskService, ModelMapper modelMapper) {
        this.groupService = groupService;
        this.adminService = adminService;
        this.taskService = taskService;
        this.modelMapper = modelMapper;
    }

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
        return groupService.findAll(auth, pageable);
    }

    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать группу", description = "Создает новую группу (только ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Группа успешно создана"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль ADMIN")
    })
    public ResponseEntity<Group> createGroup(
            @Parameter(description = "Название группы", required = true)
            @RequestParam String name,
            @Parameter(description = "Описание группы")
            @RequestParam(required = false) String description) {
        adminService.createGroup(name, description);
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
        return ResponseEntity.ok(groupService.getStudentsByGroupId(groupId));
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
        groupService.addStudentToGroup(groupId, studentId);
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
        groupService.removeStudentFromGroup(groupId, studentId);
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
        groupService.removeGroup(groupId);
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
        return ResponseEntity.ok(groupService.findById(groupId, auth));
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
        return ResponseEntity.ok(taskService.getGroupTasks(groupId));
    }

    @GetMapping("/my-students")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Получить моих студентов", description = "Возвращает список студентов, привязанных к преподавателю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка студентов",
                    content = @Content(schema = @Schema(implementation = PersonResponseDTO.class)))
    })
    public ResponseEntity<List<PersonResponseDTO>> getStudents(Authentication auth) {
        return ResponseEntity.ok(convertToResponsePerson(groupService.findByTeacherId(auth)));
    }

    @GetMapping("/students_has_group")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Получить студентов, имеющих группу", description = "Возвращает список студентов, которые уже состоят в группах")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка студентов",
                    content = @Content(schema = @Schema(implementation = PersonResponseDTO.class)))
    })
    public ResponseEntity<List<PersonResponseDTO>> getStudentsHasGroup() {
        return ResponseEntity.ok(groupService.getStudentsHasGroup());
    }

    private List<PersonResponseDTO> convertToResponsePerson(List<Person> allUsers) {
        return allUsers.stream()
                .map(user -> modelMapper.map(user, PersonResponseDTO.class))
                .collect(Collectors.toList());
    }
}