package ru.danon.spring.ToDo.controllers;

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
public class VideoMeetingController {

    private final VideoMeetingService videoMeetingService;

    @Autowired
    public VideoMeetingController(VideoMeetingService videoMeetingService) {
        this.videoMeetingService = videoMeetingService;
    }

    @GetMapping
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
    public ResponseEntity<List<VideoMeetingDTO>> getMyMeetings(Authentication authentication) {
        List<VideoMeetingDTO> meetings = videoMeetingService.getMeetingsByCreator(authentication.getName());
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<VideoMeetingDTO>> getMeetingsByGroup(@PathVariable Integer groupId) {
        List<VideoMeetingDTO> meetings = videoMeetingService.getMeetingsByGroup(groupId);
        return ResponseEntity.ok(meetings);
    }

    @GetMapping("/{meetingId}")
    public ResponseEntity<VideoMeetingDTO> getMeetingById(@PathVariable Integer meetingId) {
        VideoMeetingDTO meeting = videoMeetingService.getMeetingById(meetingId);
        return ResponseEntity.ok(meeting);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping
    public ResponseEntity<?> createMeeting(
            @Valid @RequestBody CreateVideoMeetingDTO createDTO,
            Authentication authentication) {
        try {
            System.out.println("Получен запрос на создание встречи: " + createDTO.getTitle());
            System.out.println("StartTime: " + createDTO.getStartTime());
            System.out.println("EndTime: " + createDTO.getEndTime());

            VideoMeetingDTO meeting = videoMeetingService.createMeeting(createDTO, authentication.getName());
            return ResponseEntity.ok(meeting);
        } catch (RuntimeException e) {
            e.printStackTrace(); // Логируем для отладки
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace(); // Логируем для отладки
            return ResponseEntity.badRequest().body(Map.of("message", "Ошибка при создании встречи: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{meetingId}")
    public ResponseEntity<?> updateMeeting(
            @PathVariable Integer meetingId,
            @Valid @RequestBody CreateVideoMeetingDTO updateDTO,
            Authentication authentication) {
        try {
            VideoMeetingDTO meeting = videoMeetingService.updateMeeting(meetingId, updateDTO, authentication.getName());
            return ResponseEntity.ok(meeting);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @DeleteMapping("/{meetingId}")
    public ResponseEntity<?> deleteMeeting(@PathVariable Integer meetingId, Authentication authentication) {
        try {
            videoMeetingService.deleteMeeting(meetingId, authentication.getName());
            return ResponseEntity.ok(Map.of("message", "Видеовстреча успешно удалена"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{meetingId}/join")
    public ResponseEntity<?> getJoinUrl(
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
    public ResponseEntity<?> getMeetingEmbedInfo(@PathVariable Integer meetingId, Authentication authentication) {
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

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @PostMapping("/{meetingId}/complete")
    public ResponseEntity<?> completeMeeting(@PathVariable Integer meetingId, Authentication authentication) {
        try {
            String username = authentication.getName();
            videoMeetingService.completeMeeting(meetingId, username);
            return ResponseEntity.ok(Map.of("message", "Встреча успешно завершена"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}


