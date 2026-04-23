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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.danon.spring.ToDo.dto.CreateVideoMeetingDTO;
import ru.danon.spring.ToDo.dto.EmbedInfoResponseDTO;
import ru.danon.spring.ToDo.dto.JoinUrlResponseDTO;
import ru.danon.spring.ToDo.dto.MessageResponseDTO;
import ru.danon.spring.ToDo.dto.VideoMeetingDTO;
import ru.danon.spring.ToDo.services.VideoMeetingService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/video-meetings")
@Tag(name = "Video Meeting Controller", description = "Управление видеовстречами (Jitsi Meet)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class VideoMeetingController {

    private final VideoMeetingService videoMeetingService;

    @GetMapping
    @Operation(summary = "Получить все встречи", description = "Возвращает список всех доступных видеовстреч (в зависимости от роли)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка встреч",
                    content = @Content(schema = @Schema(implementation = VideoMeetingDTO.class)))
    })
    public ResponseEntity<List<VideoMeetingDTO>> getAllMeetings(Authentication authentication) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        log.info("Запрос на получение всех встреч от пользователя: {}, роль: {}", username, role);

        List<VideoMeetingDTO> meetings;
        if (role.equals("ROLE_TEACHER") || role.equals("ROLE_ADMIN")) {
            // Преподаватели видят все встречи
            meetings = videoMeetingService.getAllMeetings();
            log.info("Преподаватель/админ {} видит {} встреч", username, meetings.size());
        } else {
            // Студенты видят встречи своей группы И встречи без группы
            meetings = videoMeetingService.getMeetingsForStudent(username);
            log.info("Студент {} видит {} встреч", username, meetings.size());
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
        log.info("Запрос на получение встреч созданных пользователем: {}", authentication.getName());
        List<VideoMeetingDTO> meetings = videoMeetingService.getMeetingsByCreator(authentication.getName());
        log.debug("Найдено {} встреч созданных пользователем {}", meetings.size(), authentication.getName());
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
        log.info("Запрос на получение встреч для группы id={}", groupId);
        List<VideoMeetingDTO> meetings = videoMeetingService.getMeetingsByGroup(groupId);
        log.debug("Найдено {} встреч для группы id={}", meetings.size(), groupId);
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
        log.info("Запрос на получение информации о встрече id={}", meetingId);
        VideoMeetingDTO meeting = videoMeetingService.getMeetingById(meetingId);
        log.debug("Информация о встрече id={} успешно получена", meetingId);
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
    public ResponseEntity<VideoMeetingDTO> createMeeting(
            @Parameter(description = "Данные для создания встречи", required = true)
            @Valid @RequestBody CreateVideoMeetingDTO createDTO,
            Authentication authentication) {

        log.info("Запрос на создание встречи: title={}, startTime={}, endTime={}, от пользователя: {}",
                createDTO.getTitle(), createDTO.getStartTime(), createDTO.getEndTime(), authentication.getName());

        VideoMeetingDTO meeting = videoMeetingService.createMeeting(createDTO, authentication.getName());
        log.info("Встреча успешно создана: id={}, title={}", meeting.getId(), meeting.getTitle());
        return ResponseEntity.ok(meeting);
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
    public ResponseEntity<VideoMeetingDTO> updateMeeting(
            @Parameter(description = "ID встречи", required = true)
            @PathVariable Integer meetingId,
            @Parameter(description = "Обновленные данные встречи", required = true)
            @Valid @RequestBody CreateVideoMeetingDTO updateDTO,
            Authentication authentication) {

        log.info("Запрос на обновление встречи id={} от пользователя: {}", meetingId, authentication.getName());
        VideoMeetingDTO meeting = videoMeetingService.updateMeeting(meetingId, updateDTO, authentication.getName());
        log.info("Встреча id={} успешно обновлена", meetingId);
        return ResponseEntity.ok(meeting);
    }

    @DeleteMapping("/{meetingId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Удалить встречу", description = "Удаляет видеовстречу (только для TEACHER или ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Встреча успешно удалена"),
            @ApiResponse(responseCode = "400", description = "Ошибка удаления встречи"),
            @ApiResponse(responseCode = "404", description = "Встреча не найдена")
    })
    public ResponseEntity<MessageResponseDTO> deleteMeeting(
            @Parameter(description = "ID встречи", required = true)
            @PathVariable Integer meetingId,
            Authentication authentication) {

        log.info("Запрос на удаление встречи id={} от пользователя: {}", meetingId, authentication.getName());
        videoMeetingService.deleteMeeting(meetingId, authentication.getName());
        log.info("Встреча id={} успешно удалена пользователем {}", meetingId, authentication.getName());
        return ResponseEntity.ok(new MessageResponseDTO("Видеовстреча успешно удалена"));
    }

    @GetMapping("/{meetingId}/join")
    @Operation(summary = "Получить ссылку для присоединения", description = "Возвращает URL для присоединения к видеовстрече")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ссылка успешно получена",
                    content = @Content(schema = @Schema(example = "{\"joinUrl\": \"https://meet.jit.si/meeting-123\"}"))),
            @ApiResponse(responseCode = "400", description = "Ошибка получения ссылки"),
            @ApiResponse(responseCode = "404", description = "Встреча не найдена")
    })
    public ResponseEntity<JoinUrlResponseDTO> getJoinUrl(
            @Parameter(description = "ID встречи", required = true)
            @PathVariable Integer meetingId,
            Authentication authentication) {

        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        boolean isModerator = role.equals("ROLE_TEACHER") || role.equals("ROLE_ADMIN");

        log.info("Запрос на получение ссылки для присоединения к встрече id={} от пользователя: {}, модератор: {}",
                meetingId, username, isModerator);
        String joinUrl = videoMeetingService.getJoinUrl(meetingId, username, isModerator);
        log.debug("Ссылка для присоединения к встрече id={} успешно сгенерирована", meetingId);
        return ResponseEntity.ok(new JoinUrlResponseDTO(joinUrl));
    }

    @GetMapping("/{meetingId}/embed")
    @Operation(summary = "Получить embed информацию", description = "Возвращает информацию для встраивания видеовстречи")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение embed информации",
                    content = @Content(schema = @Schema(example = "{\"meeting\": {...}, \"embedUrl\": \"...\", \"isModerator\": false, \"userName\": \"student\"}"))),
            @ApiResponse(responseCode = "400", description = "Ошибка получения информации")
    })
    public ResponseEntity<EmbedInfoResponseDTO> getMeetingEmbedInfo(
            @Parameter(description = "ID встречи", required = true)
            @PathVariable Integer meetingId,
            Authentication authentication) {

        VideoMeetingDTO meeting = videoMeetingService.getMeetingById(meetingId);
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        boolean isModerator = role.equals("ROLE_TEACHER") || role.equals("ROLE_ADMIN");

        log.info("Запрос на получение embed информации для встречи id={} от пользователя: {}, модератор: {}",
                meetingId, username, isModerator);

        // Генерируем embed URL
        String embedUrl = videoMeetingService.getEmbedUrl(meeting.getMeetingId(), username, isModerator);
        log.debug("Embed URL для встречи id={} успешно сгенерирован", meetingId);

        EmbedInfoResponseDTO response = new EmbedInfoResponseDTO();
        response.setMeeting(meeting);
        response.setEmbedUrl(embedUrl);
        response.setModerator(isModerator);
        response.setUserName(username);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{meetingId}/complete")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Завершить встречу", description = "Отмечает видеовстречу как завершенную (только для TEACHER или ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Встреча успешно завершена"),
            @ApiResponse(responseCode = "400", description = "Ошибка завершения встречи"),
            @ApiResponse(responseCode = "404", description = "Встреча не найдена")
    })
    public ResponseEntity<MessageResponseDTO> completeMeeting(
            @Parameter(description = "ID встречи", required = true)
            @PathVariable Integer meetingId,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Запрос на завершение встречи id={} от пользователя: {}", meetingId, username);
        videoMeetingService.completeMeeting(meetingId, username);
        log.info("Встреча id={} успешно завершена пользователем {}", meetingId, username);
        return ResponseEntity.ok(new MessageResponseDTO("Встреча успешно завершена"));
    }

}