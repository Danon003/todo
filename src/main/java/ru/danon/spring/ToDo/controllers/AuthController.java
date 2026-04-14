package ru.danon.spring.ToDo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.danon.spring.ToDo.dto.AuthenticationDTO;
import ru.danon.spring.ToDo.dto.ForgotPasswordRequest;
import ru.danon.spring.ToDo.dto.PersonDTO;
import ru.danon.spring.ToDo.dto.ResetPasswordRequest;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.security.JWTUtil;
import ru.danon.spring.ToDo.services.RegistrationService;
import ru.danon.spring.ToDo.controllers.validators.PersonValidator;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Authentication Controller", description = "Аутентификация и регистрация пользователей")
@Slf4j
public class AuthController {
    private final PersonValidator personValidator;
    private final RegistrationService registrationServiceImpl;
    private final JWTUtil jwtUtil;
    private final ModelMapper modelMapper;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/registration")
    @Operation(summary = "Регистрация нового пользователя", description = "Создает нового пользователя и возвращает JWT токен")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная регистрация",
                    content = @Content(schema = @Schema(example = "{\"jwt-token\": \"eyJhbGciOiJIUzI1NiIs...\"}"))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или пароль не указан",
                    content = @Content(schema = @Schema(example = "{\"message\": \"Пароль обязателен для регистрации!\"}")))
    })
    public Map<String, String> performRegistration(
            @Parameter(description = "Данные для регистрации пользователя", required = true)
            @RequestBody @Valid PersonDTO personDTO,
            BindingResult bindingResult) {

        log.info("Запрос на регистрацию нового пользователя: {}", personDTO.getUsername());

        // Проверяем, что пароль указан при регистрации
        if (personDTO.getPassword() == null || personDTO.getPassword().trim().isEmpty()) {
            log.warn("Попытка регистрации без пароля для пользователя: {}", personDTO.getUsername());
            return Map.of("message", "Пароль обязателен для регистрации!");
        }

        Person person = convertToPerson(personDTO);

        personValidator.validate(person, bindingResult);

        if (bindingResult.hasErrors()) {
            log.warn("Ошибка валидации при регистрации пользователя {}: {}", personDTO.getUsername(), bindingResult.getAllErrors());
            return Map.of("message", "Error!");
        }

        registrationServiceImpl.register(person);
        String token = jwtUtil.generateToken(personDTO.getUsername());
        log.info("Пользователь {} успешно зарегистрирован", personDTO.getUsername());

        return Map.of("jwt-token", token);
    }

    @PostMapping("/login")
    @Operation(summary = "Вход в систему", description = "Аутентификация пользователя и получение JWT токена")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный вход",
                    content = @Content(schema = @Schema(example = "{\"jwt-token\": \"eyJhbGciOiJIUzI1NiIs...\"}"))),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные",
                    content = @Content(schema = @Schema(example = "{\"message\": \"Incorrect credentials!\"}")))
    })
    public Map<String, String> performLogin(
            @Parameter(description = "Учетные данные пользователя", required = true)
            @RequestBody AuthenticationDTO authenticationDTO) {
        log.info("Попытка входа пользователя: {}", authenticationDTO.getUsername());

        UsernamePasswordAuthenticationToken authInputToken =
                new UsernamePasswordAuthenticationToken(authenticationDTO.getUsername(), authenticationDTO.getPassword());

        authenticationManager.authenticate(authInputToken);
        log.info("Пользователь {} успешно аутентифицирован", authenticationDTO.getUsername());


        String token = jwtUtil.generateToken(authenticationDTO.getUsername());
        return Map.of("jwt-token", token);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Запрос на восстановление пароля", description = "Отправляет код восстановления на email пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Код восстановления отправлен"),
            @ApiResponse(responseCode = "400", description = "Ошибка отправки кода")
    })
    public ResponseEntity<?> performForgotPassword(
            @Parameter(description = "Email пользователя для восстановления пароля", required = true)
            @RequestBody ForgotPasswordRequest request) {

        log.info("Запрос на восстановление пароля для email: {}", request.getEmail());
        registrationServiceImpl.initiatePasswordReset(request.getEmail());
        log.info("Код восстановления отправлен на email: {}", request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Сброс пароля", description = "Сбрасывает пароль с использованием полученного кода")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пароль успешно изменен"),
            @ApiResponse(responseCode = "400", description = "Ошибка сброса пароля (неверный код или email)")
    })
    public ResponseEntity<?> performResetPassword(
            @Parameter(description = "Данные для сброса пароля", required = true)
            @RequestBody ResetPasswordRequest request) {

        log.info("Запрос на сброс пароля для email: {}", request.getEmail());
        registrationServiceImpl.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        log.info("Пароль успешно сброшен для email: {}", request.getEmail());
        return ResponseEntity.ok().build();
    }

    public Person convertToPerson(PersonDTO personDTO) {
        return modelMapper.map(personDTO, Person.class);
    }
}