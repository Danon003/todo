package ru.danon.spring.ToDo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.danon.spring.ToDo.dto.AuthenticationDTO;
import ru.danon.spring.ToDo.dto.ForgotPasswordRequest;
import ru.danon.spring.ToDo.dto.PersonDTO;
import ru.danon.spring.ToDo.dto.ResetPasswordRequest;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.security.JWTUtil;
import ru.danon.spring.ToDo.services.RegistrationService;
import ru.danon.spring.ToDo.util.PersonValidator;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication Controller", description = "Аутентификация и регистрация пользователей")
public class AuthController {
    private final PersonValidator personValidator;
    private final RegistrationService registrationService;
    private final JWTUtil jwtUtil;
    private final ModelMapper modelMapper;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthController(PersonValidator personValidator, RegistrationService registrationService, JWTUtil jwtUtil, ModelMapper modelMapper, AuthenticationManager authenticationManager) {
        this.personValidator = personValidator;
        this.registrationService = registrationService;
        this.jwtUtil = jwtUtil;
        this.modelMapper = modelMapper;
        this.authenticationManager = authenticationManager;
    }

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

        // Проверяем, что пароль указан при регистрации
        if (personDTO.getPassword() == null || personDTO.getPassword().trim().isEmpty()) {
            return Map.of("message", "Пароль обязателен для регистрации!");
        }

        Person person = convertToPerson(personDTO);

        personValidator.validate(person, bindingResult);

        if (bindingResult.hasErrors()) {
            return Map.of("message", "Error!");
        }

        registrationService.register(person);
        String token = jwtUtil.generateToken(personDTO.getUsername());

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
        UsernamePasswordAuthenticationToken authInputToken =
                new UsernamePasswordAuthenticationToken(authenticationDTO.getUsername(), authenticationDTO.getPassword());

        try {
            authenticationManager.authenticate(authInputToken);
        } catch (BadCredentialsException e) {
            return Map.of("message", "Incorrect credentials!");
        }

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
        try {
            registrationService.initiatePasswordReset(request.getEmail());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка отправки кода");
        }
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
        try {
            registrationService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка сброса пароля");
        }
    }

    public Person convertToPerson(PersonDTO personDTO) {
        return modelMapper.map(personDTO, Person.class);
    }
}