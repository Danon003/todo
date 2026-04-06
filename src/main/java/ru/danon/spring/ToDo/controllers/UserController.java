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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.danon.spring.ToDo.dto.GroupResponseDTO;
import ru.danon.spring.ToDo.dto.PersonDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.PeopleService;

import java.util.Map;

@RestController
@RequestMapping("/user")
@Tag(name = "User Controller", description = "Управление профилем пользователя")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final PeopleService peopleService;
    private final GroupService groupService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserController(PeopleService peopleService, GroupService groupService, PasswordEncoder passwordEncoder) {
        this.peopleService = peopleService;
        this.groupService = groupService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me/info")
    @Operation(summary = "Получить информацию о себе", description = "Возвращает информацию о текущем авторизованном пользователе")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение информации",
                    content = @Content(schema = @Schema(implementation = PersonResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    public ResponseEntity<PersonResponseDTO> getUserInfo(Authentication authentication) {
        return ResponseEntity.ok(peopleService.getUserInfo(authentication.getName()));
    }

    @GetMapping("/my-group")
    @Operation(summary = "Получить мою группу", description = "Возвращает информацию о группе, в которой состоит пользователь")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение информации о группе",
                    content = @Content(schema = @Schema(implementation = GroupResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Группа не найдена")
    })
    public ResponseEntity<GroupResponseDTO> getMyGroup(Authentication authentication) {
        return ResponseEntity.ok(groupService.getGroupInfo(authentication));
    }

    @GetMapping("/about-user/{id}")
    @Operation(summary = "Получить информацию о пользователе", description = "Возвращает информацию о пользователе по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение информации",
                    content = @Content(schema = @Schema(example = "{\"teacherName\": \"Иван Петров\"}"))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public Map<String, String> getAboutUser(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable Integer id) {
        Person person = peopleService.findById(id).orElseThrow(() -> new RuntimeException("Person not found"));
        return Map.of("teacherName", person.getUsername());
    }

    @PutMapping("/me/update")
    @Operation(summary = "Обновить профиль", description = "Обновляет информацию профиля текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Профиль успешно обновлен",
                    content = @Content(schema = @Schema(implementation = PersonResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации данных"),
            @ApiResponse(responseCode = "409", description = "Email или имя пользователя уже заняты")
    })
    public ResponseEntity<?> updateProfile(
            @Parameter(description = "Обновленные данные пользователя", required = true)
            @Valid @RequestBody PersonDTO personDTO,
            Authentication authentication) {
        try {
            // Хешируем пароль, если он указан
            String encodedPassword = null;
            if (personDTO.getPassword() != null && !personDTO.getPassword().trim().isEmpty()) {
                encodedPassword = passwordEncoder.encode(personDTO.getPassword());
            }

            PersonResponseDTO updatedUser = peopleService.updateUserProfile(
                    authentication.getName(),
                    personDTO.getUsername(),
                    personDTO.getEmail(),
                    encodedPassword
            );
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}