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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.danon.spring.ToDo.dto.AboutUserResponseDTO;
import ru.danon.spring.ToDo.dto.GroupResponseDTO;
import ru.danon.spring.ToDo.dto.PersonDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.mappers.PersonMapper;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.PeopleService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Tag(name = "User Controller", description = "Управление профилем пользователя")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class UserController {

    private final PeopleService peopleService;
    private final GroupService groupService;
    private final PasswordEncoder passwordEncoder;
    private final PersonMapper personMapper;

    @GetMapping("/me/info")
    @Operation(summary = "Получить информацию о себе", description = "Возвращает информацию о текущем авторизованном пользователе")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение информации",
                    content = @Content(schema = @Schema(implementation = PersonResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    public ResponseEntity<PersonResponseDTO> getUserInfo(Authentication authentication) {
        log.info("Запрос на получение информации о пользователе: {}", authentication.getName());
        PersonResponseDTO userInfo = peopleService.getUserInfo(authentication.getName());
        log.debug("Информация о пользователе {} успешно получена", authentication.getName());
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/my-group")
    @Operation(summary = "Получить мою группу", description = "Возвращает информацию о группе, в которой состоит пользователь")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение информации о группе",
                    content = @Content(schema = @Schema(implementation = GroupResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Группа не найдена")
    })
    public ResponseEntity<GroupResponseDTO> getMyGroup(Authentication authentication) {
        log.info("Запрос на получение группы пользователя: {}", authentication.getName());
        GroupResponseDTO group = groupService.getGroupInfo(authentication);
        log.debug("Информация о группе пользователя {} успешно получена", authentication.getName());
        return ResponseEntity.ok(group);
    }

    @GetMapping("/about-user/{id}")
    @Operation(summary = "Получить информацию о пользователе", description = "Возвращает информацию о пользователе по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение информации",
                    content = @Content(schema = @Schema(example = "{\"teacherName\": \"Иван Петров\"}"))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<AboutUserResponseDTO> getAboutUser(
            @Parameter(description = "ID пользователя", required = true)
            @PathVariable Integer id) {
        log.info("Запрос на получение информации о пользователе id={}", id);
        Person person = peopleService.findById(id).orElseThrow(() -> {
            log.error("Пользователь с id={} не найден", id);
            return new EntityNotFoundException("Person not found", id);
        });
        log.debug("Информация о пользователе id={} успешно получена", id);
        return ResponseEntity.ok(personMapper.toAboutUserDto(person));
    }

    @PutMapping("/me/update")
    @Operation(summary = "Обновить профиль", description = "Обновляет информацию профиля текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Профиль успешно обновлен",
                    content = @Content(schema = @Schema(implementation = PersonResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации данных"),
            @ApiResponse(responseCode = "409", description = "Email или имя пользователя уже заняты")
    })
    public ResponseEntity<PersonResponseDTO> updateProfile(
            @Parameter(description = "Обновленные данные пользователя", required = true)
            @Valid @RequestBody PersonDTO personDTO,
            Authentication authentication) {

        log.info("Запрос на обновление профиля пользователя: {}", authentication.getName());

        // Хешируем пароль, если он указан
        String encodedPassword = null;
        if (personDTO.getPassword() != null && !personDTO.getPassword().trim().isEmpty()) {
            log.debug("Пользователь {} обновляет пароль", authentication.getName());
            encodedPassword = passwordEncoder.encode(personDTO.getPassword());
        }

        PersonResponseDTO updatedUser = peopleService.updateUserProfile(
                authentication.getName(),
                personDTO.getUsername(),
                personDTO.getEmail(),
                encodedPassword
        );
        log.info("Профиль пользователя {} успешно обновлен", authentication.getName());
        return ResponseEntity.ok(updatedUser);
    }
}