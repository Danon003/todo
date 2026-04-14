package ru.danon.spring.ToDo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.danon.spring.ToDo.dto.DashboardStatsDTO;
import ru.danon.spring.ToDo.dto.IdDTO;
import ru.danon.spring.ToDo.dto.LogResponseDTO;
import ru.danon.spring.ToDo.dto.PersonDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.services.AdminService;
import ru.danon.spring.ToDo.services.PeopleService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@Tag(name = "Admin Controller", description = "Управление пользователями, ролями, группами и статистикой (только для администраторов и преподавателей)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final PeopleService peopleService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Получить список всех пользователей", description = "Возвращает страницу со списком пользователей. Доступно для ADMIN и TEACHER")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка пользователей",
                    content = @Content(schema = @Schema(implementation = PersonResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен - недостаточно прав")
    })
    public Page<PersonResponseDTO> getAllUsers(
            @Parameter(description = "Параметры пагинации (size, page, sort)")
            @PageableDefault(size = 15) Pageable page
    ) {
        log.info("Запрос на получение списка всех пользователей с пагинацией: page={}, size={}", page.getPageNumber(), page.getPageSize());
        Page<Person> usersPage = adminService.getAllUsers(page);
        log.debug("Получено {} пользователей из {} всего", usersPage.getNumberOfElements(), usersPage.getTotalElements());
        return usersPage.map(person -> modelMapper.map(person, PersonResponseDTO.class));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать нового пользователя", description = "Создает нового пользователя с ролью STUDENT. Доступно только для ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно создан",
                    content = @Content(schema = @Schema(implementation = Person.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные или email уже используется",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль ADMIN")
    })
    public ResponseEntity<?> createUser(@Valid @RequestBody PersonDTO personDTO) {
        log.info("Запрос на создание нового пользователя с email: {}", personDTO.getEmail());
        if (peopleService.findByEmail(personDTO.getEmail()).isPresent()) {
            log.warn("Попытка создания пользователя с уже существующим email: {}", personDTO.getEmail());
            return ResponseEntity.badRequest().body("Email already in use");
        }

        Person person = convertToPerson(personDTO);
        person.setPassword(passwordEncoder.encode(person.getPassword()));
        person.setRole("ROLE_STUDENT");
        person.setCreatedAt(LocalDateTime.now());

        Person savedPerson = peopleService.save(person);
        log.info("Пользователь успешно создан: id={}, email={}", savedPerson.getId(), savedPerson.getEmail());
        return ResponseEntity.ok(savedPerson);
    }

    @DeleteMapping("/users/{userId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя по ID. Доступно только для ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Пользователь успешно удален"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<Void> deleteUser(@Parameter(description = "ID пользователя", required = true)
                                           @PathVariable Integer userId) {
        log.info("Запрос на удаление пользователя с id: {}", userId);
        peopleService.deleteById(userId);
        log.info("Пользователь с id {} успешно удален", userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Изменить роль пользователя", description = "Изменяет роль пользователя. Доступно только для ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Роль успешно изменена"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<Void> changeUserRole(
            @Parameter(description = "Новая роль пользователя", required = true, example = "TEACHER")
            @RequestParam String role,
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable Integer userId) {

        log.info("Запрос на изменение роли пользователя id={} на {}", userId, role);
        adminService.changeUserRole(userId, role.toUpperCase());
        log.info("Роль пользователя id={} успешно изменена на {}", userId, role);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Назначить пользователя преподавателем", description = "Повышает пользователя до роли TEACHER. Доступно только для ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь успешно назначен преподавателем",
                    content = @Content(schema = @Schema(implementation = Person.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<Person> createTeachers(@RequestBody IdDTO id) {
        log.info("Запрос на назначение пользователя id={} преподавателем", id.getId());
        Person teacher = adminService.createTeacher(id.getId());
        log.info("Пользователь id={} успешно назначен преподавателем", id.getId());
        return ResponseEntity.ok(teacher);
    }

    @GetMapping("/users/by-role")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(summary = "Получить пользователей по роли", description = "Возвращает страницу пользователей с указанной ролью. Доступно для ADMIN и TEACHER")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка пользователей",
                    content = @Content(schema = @Schema(implementation = PersonResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен - недостаточно прав")
    })
    public ResponseEntity<Page<PersonResponseDTO>> getUserByRole(
            @Parameter(description = "Параметры пагинации (size, page, sort)")
            @PageableDefault(size = 15) Pageable page,
            @Parameter(description = "Роль пользователя", required = true, example = "STUDENT")
            @RequestParam String role) {
        log.info("Запрос на получение пользователей с ролью: {}, page={}, size={}", role, page.getPageNumber(), page.getPageSize());
        Page<Person> usersPage = adminService.getUsersByRole(role, page);
        log.debug("Найдено {} пользователей с ролью {} из {} всего", usersPage.getNumberOfElements(), role, usersPage.getTotalElements());
        return ResponseEntity.ok(usersPage.map(person -> modelMapper.map(person, PersonResponseDTO.class)));
    }

    @GetMapping("/role-audit-log")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить историю изменений ролей", description = "Возвращает список всех изменений ролей пользователей. Доступно только для ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение логов",
                    content = @Content(schema = @Schema(implementation = LogResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль ADMIN")
    })
    public ResponseEntity<List<LogResponseDTO>> getRoleAuditLog() {
        log.info("Запрос на получение истории изменений ролей");
        List<LogResponseDTO> auditLogs = adminService.getRoleAuditLogs();
        log.debug("Получено {} записей аудита ролей", auditLogs.size());
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/statistic")
    @Operation(summary = "Получить статистику дашборда", description = "Возвращает статистическую информацию для дашборда. Доступность зависит от роли пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение статистики",
                    content = @Content(schema = @Schema(implementation = DashboardStatsDTO.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    public ResponseEntity<DashboardStatsDTO> getStatistic(Authentication auth) {
        log.info("Запрос на получение статистики дашборда от пользователя: {}", auth.getName());
        DashboardStatsDTO stats = adminService.getDashboardStats(auth);
        log.debug("Статистика дашборда успешно сформирована для пользователя: {}", auth.getName());
        return ResponseEntity.ok(stats);
    }

    @PutMapping("/{groupId}/teacher/{teacherId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Назначить преподавателя группе", description = "Привязывает преподавателя к указанной группе. Доступно только для ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Преподаватель успешно назначен группе"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Группа или преподаватель не найдены")
    })
    public ResponseEntity<Void> assignTeacherToGroup(
            @Parameter(description = "ID группы", required = true)
            @PathVariable Integer groupId,
            @Parameter(description = "ID преподавателя", required = true)
            @PathVariable Integer teacherId) {

        log.info("Запрос на назначение преподавателя id={} группе id={}", teacherId, groupId);
        adminService.assignTeacherToGroup(groupId, teacherId);
        log.info("Преподаватель id={} успешно назначен группе id={}", teacherId, groupId);
        return ResponseEntity.noContent().build();
    }

    private Person convertToPerson(PersonDTO personDTO) {
        return modelMapper.map(personDTO, Person.class);
    }

    private List<PersonResponseDTO> convertToResponsePerson(List<Person> allUsers) {
        return allUsers.stream()
                .map(user -> modelMapper.map(user, PersonResponseDTO.class))
                .collect(Collectors.toList());
    }
}