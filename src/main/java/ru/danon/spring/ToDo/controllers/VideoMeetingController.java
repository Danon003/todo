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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.danon.spring.ToDo.dto.CreateVideoMeetingDTO;
import ru.danon.spring.ToDo.dto.VideoMeetingDTO;
import ru.danon.spring.ToDo.services.VideoMeetingService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/video-meetings")
@Tag(name = "Video Meeting Controller", description = "Управление видеовстречами (Jitsi Meet)")
@SecurityRequirement(name = "bearerAuth")
public class VideoMeetingController {

    private final VideoMeetingService videoMeetingService;

    @Autowired
    public VideoMeetingController(VideoMeetingService videoMeetingService) {
        this.videoMeetingService = videoMeetingService;
    }

    @GetMapping
    @Operation(summary = "Получить все встречи", description = "Возвращает список всех доступных видеовстреч (в зависимости от роли)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка встреч",
                    content = @Content(schema = @Schema(implementation = VideoMeetingDTO.class)))
    })
    public ResponseEntity<List<VideoMeetingDTO>> getAllMeetings(Authentication authentication) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        List<VideoMeetingDTO> meetings;
        if (role.equals("ROLE_TEACHER") || role.equals("ROLE_ADMIN")) {
            // Преподаватели видят все встречи
            meetings = videoMeetingService.getAllMeetings();
            System.out.println("👨‍🏫 Преподаватель " + username + " видит " + meetings.size() + " встреч");
        } else {
            // Студенты видят встречи своей группы И встречи без группы
            meetings = videoMeetingService.getMeetingsForStudent(username);
            System.out.println("🎓 Студент " + username + " видит " + meetings.size() + " встреч");
        }

        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/my-meetings")
    @Operation(summary = "Получить мои встречи", description = "Возвращает список встреч, созданных текущим пользователем")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка встреч",
                    content = @Content(schema = @Schema(implementation = VideoMeetingDTO.class)))
    })
    public ResponseEntity<List<VideoMeetingDTO>> getMyMeetings(Authentication authentication) {
        List<VideoMeetingDTO> meetings = videoMeetingService.getMeetingsByCreator(authentication.getName());
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Получить встречи группы", description = "Возвращает список видеовстреч, назначенных на группу")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка встреч",
                    content = @Content(schema = @Schema(implementation = VideoMeetingDTO.class)))
    })
    public ResponseEntity<List<VideoMeetingDTO>> getMeetingsByGroup(
            @Parameter(description = "ID группы", required = true)
            @PathVariable Integer groupId) {
        List<VideoMeetingDTO> meetings = videoMeetingService.getMeetingsByGroup(groupId);
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/{meetingId}")
    @Operation(summary = "Получить встречу по ID", description = "Возвращает детальную информацию о видеовстрече")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение информации о встрече",
                    content = @Content(schema = @Schema(implementation = VideoMeetingDTO.class))),
            @ApiResponse(responseCode = "404", description = "Встреча не найдена")
    })
    public ResponseEntity<VideoMeetingDTO> getMeetingById(
            @Parameter(description = "ID встречи", required = true)
            @PathVariable Integer meetingId) {
        VideoMeetingDTO meeting = videoMeetingService.getMeetingById(meetingId);
        return ResponseEntity.ok(meeting);
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Создать встречу", description = "Создает новую видеовстречу (только для TEACHER)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Встреча успешно создана",
                    content = @Content(schema = @Schema(implementation = VideoMeetingDTO.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные встречи"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль TEACHER")
    })
    public ResponseEntity<?> createMeeting(
            @Parameter(description = "Данные для создания встречи", required = true)
            @Valid @RequestBody CreateVideoMeetingDTO createDTO,
            Authentication authentication) {
        try {
            System.out.println("Получен запрос на создание встречи: " + createDTO.getTitle());
            System.out.println("StartTime: " + createDTO.getStartTime());
            System.out.println("EndTime: " + createDTO.getEndTime());

            VideoMeetingDTO meeting = videoMeetingService.createMeeting(createDTO, authentication.getName());
            return ResponseEntity.ok(meeting);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Ошибка при создании встречи: " + e.getMessage()));
        }
    }

    @PutMapping("/{meetingId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Обновить встречу", description = "Обновляет информацию о видеовстрече (только для TEACHER)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Встреча успешно обновлена",
                    content = @Content(schema = @Schema(implementation = VideoMeetingDTO.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "404", description = "Встреча не найдена")
    })
    public ResponseEntity<?> updateMeeting(
            @Parameter(description = "ID встречи", required = true)
            @PathVariable Integer meetingId,
            @Parameter(description = "Обновленные данные встречи", required = true)
            @Valid @RequestBody CreateVideoMeetingDTO updateDTO,
            Authentication authentication) {
        try {
            VideoMeetingDTO meeting = videoMeetingService.updateMeeting(meetingId, updateDTO, authentication.getName());
            return ResponseEntity.ok(meeting);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{meetingId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Удалить встречу", description = "Удаляет видеовстречу (только для TEACHER или ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Встреча успешно удалена"),
            @ApiResponse(responseCode = "400", description = "Ошибка удаления встречи"),
            @ApiResponse(responseCode = "404", description = "Встреча не найдена")
    })
    public ResponseEntity<?> deleteMeeting(
            @Parameter(description = "ID встречи", required = true)
            @PathVariable Integer meetingId,
            Authentication authentication) {
        try {
            videoMeetingService.deleteMeeting(meetingId, authentication.getName());
            return ResponseEntity.ok(Map.of("message", "Видеовстреча успешно удалена"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{meetingId}/join")
    @Operation(summary = "Получить ссылку для присоединения", description = "Возвращает URL для присоединения к видеовстрече")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ссылка успешно получена",
                    content = @Content(schema = @Schema(example = "{\"joinUrl\": \"https://meet.jit.si/meeting-123\"}"))),
            @ApiResponse(responseCode = "400", description = "Ошибка получения ссылки"),
            @ApiResponse(responseCode = "404", description = "Встреча не найдена")
    })
    public ResponseEntity<?> getJoinUrl(
            @Parameter(description = "ID встречи", required = true)
            @PathVariable Integer meetingId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            String role = authentication.getAuthorities().iterator().next().getAuthority();
            boolean isModerator = role.equals("ROLE_TEACHER") || role.equals("ROLE_ADMIN");

            String joinUrl = videoMeetingService.getJoinUrl(meetingId, username, isModerator);
            return ResponseEntity.ok(Map.of("joinUrl", joinUrl));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{meetingId}/embed")
    @Operation(summary = "Получить embed информацию", description = "Возвращает информацию для встраивания видеовстречи")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение embed информации",
                    content = @Content(schema = @Schema(example = "{\"meeting\": {...}, \"embedUrl\": \"...\", \"isModerator\": false, \"userName\": \"student\"}"))),
            @ApiResponse(responseCode = "400", description = "Ошибка получения информации")
    })
    public ResponseEntity<?> getMeetingEmbedInfo(
            @Parameter(description = "ID встречи", required = true)
            @PathVariable Integer meetingId,
            Authentication authentication) {
        try {
            VideoMeetingDTO meeting = videoMeetingService.getMeetingById(meetingId);
            String username = authentication.getName();
            String role = authentication.getAuthorities().iterator().next().getAuthority();
            boolean isModerator = role.equals("ROLE_TEACHER") || role.equals("ROLE_ADMIN");

            // Генерируем embed URL
            String embedUrl = generateJitsiEmbedUrl(meeting.getMeetingId(), username, isModerator);

            return ResponseEntity.ok(Map.of(
                    "meeting", meeting,
                    "embedUrl", embedUrl,
                    "isModerator", isModerator,
                    "userName", username
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Генерирует URL для embed Jitsi Meet
     */
    private String generateJitsiEmbedUrl(String meetingId, String userName, boolean isModerator) {
        return "https://meet.jit.si/" + meetingId +
                "#config.prejoinPageEnabled=false" + // Пропускаем страницу присоединения
                "&userInfo.displayName=" + URLEncoder.encode(userName, StandardCharsets.UTF_8) +
                "&interfaceConfig.DEFAULT_BACKGROUND=\"#ffffff\"" +
                "&config.disableModeratorIndicator=" + !isModerator +
                "&config.startWithAudioMuted=true" +
                "&config.startWithVideoMuted=false";
    }

    @PostMapping("/{meetingId}/complete")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Завершить встречу", description = "Отмечает видеовстречу как завершенную (только для TEACHER или ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Встреча успешно завершена"),
            @ApiResponse(responseCode = "400", description = "Ошибка завершения встречи"),
            @ApiResponse(responseCode = "404", description = "Встреча не найдена")
    })
    public ResponseEntity<?> completeMeeting(
            @Parameter(description = "ID встречи", required = true)
            @PathVariable Integer meetingId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            videoMeetingService.completeMeeting(meetingId, username);
            return ResponseEntity.ok(Map.of("message", "Встреча успешно завершена"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}