package ru.danon.spring.ToDo.services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.CreateVideoMeetingDTO;
import ru.danon.spring.ToDo.dto.VideoMeetingDTO;
import ru.danon.spring.ToDo.models.Group;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.models.VideoMeeting;
import ru.danon.spring.ToDo.repositories.jpa.GroupRepository;
import ru.danon.spring.ToDo.repositories.jpa.PeopleRepository;
import ru.danon.spring.ToDo.repositories.jpa.VideoMeetingRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class VideoMeetingService {

    private final VideoMeetingRepository videoMeetingRepository;
    private final PeopleRepository peopleRepository;
    private final GroupRepository groupRepository;
    private final JitsiMeetService jitsiMeetService;
    private final ModelMapper modelMapper;
    private final GroupService groupService;
    private final NotificationProducerService notificationProducerService;
    private final long cleanupAfterDays;

    @Autowired
    public VideoMeetingService(
            VideoMeetingRepository videoMeetingRepository,
            PeopleRepository peopleRepository,
            GroupRepository groupRepository,
            JitsiMeetService jitsiMeetService, // ЗАМЕНИЛИ ТУТ
            ModelMapper modelMapper,
            GroupService groupService,
            NotificationProducerService notificationProducerService,
            @Value("${video.meetings.cleanup-after-days:14}") long cleanupAfterDays) {
        this.videoMeetingRepository = videoMeetingRepository;
        this.peopleRepository = peopleRepository;
        this.groupRepository = groupRepository;
        this.jitsiMeetService = jitsiMeetService; // И ТУТ
        this.modelMapper = modelMapper;
        this.groupService = groupService;
        this.notificationProducerService = notificationProducerService;
        this.cleanupAfterDays = cleanupAfterDays;
    }

    @Transactional
    public VideoMeetingDTO createMeeting(CreateVideoMeetingDTO createDTO, String username) {
        Person creator = peopleRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!"ROLE_TEACHER".equals(creator.getRole())) {
            throw new RuntimeException("Назначать видеовстречи может только преподаватель");
        }

        VideoMeeting meeting = new VideoMeeting();
        meeting.setTitle(createDTO.getTitle());
        meeting.setDescription(createDTO.getDescription());
        meeting.setStartTime(createDTO.getStartTime());
        meeting.setEndTime(createDTO.getEndTime());
        meeting.setCreatedBy(creator);
        meeting.setCreatedAt(LocalDateTime.now());
        meeting.setUpdatedAt(LocalDateTime.now());
        meeting.setIsActive(true);
        meeting.setReminderSent(false);

        if (createDTO.getGroupId() != null) {
            Optional<Group> group = groupRepository.findById(createDTO.getGroupId());
            group.ifPresent(meeting::setGroup);
        }

        // ✅ СОЗДАЕМ ВСТРЕЧУ ЧЕРЕЗ JITSI MEET (просто и надежно)
        Map<String, String> meetResult = jitsiMeetService.createMeeting(
                createDTO.getTitle(),
                createDTO.getDescription()
        );
        meeting.setMeetingUrl(meetResult.get("meetingUrl"));
        meeting.setMeetingId(meetResult.get("meetingId"));

        VideoMeeting savedMeeting = videoMeetingRepository.save(meeting);
        notifyMeetingCreated(savedMeeting);
        return convertToDTO(savedMeeting);
    }
    /**
     * Получает все встречи с учетом роли пользователя
     */
    public List<VideoMeetingDTO> getAllMeetings(Authentication authentication) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        Person user = peopleRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (role.equals("ROLE_TEACHER") || role.equals("ROLE_ADMIN")) {
            // Преподаватели видят все активные встречи
            return videoMeetingRepository.findAll().stream()
                    .filter(meeting -> meeting.getIsActive() != null && meeting.getIsActive())
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            // Студенты видят встречи своей группы ИЛИ встречи без группы
            return getMeetingsForStudent(user.getUsername());
        }
    }

    /**
     * Получает встречи доступные студенту
     * - встречи без группы (для всех)
     * - встречи группы студента
     */
    public List<VideoMeetingDTO> getMeetingsForStudent(String username) {
        try {
            Person student = peopleRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Студент не найден: " + username));

            System.out.println("🎓 Поиск встреч для студента: " + username + ", ID: " + student.getId());

            // Получаем группу студента
            Integer studentGroupId = getStudentGroupId(student);
            System.out.println("🎓 Группа студента: " + studentGroupId);

            // Находим встречи: без группы ИЛИ для группы студента
            List<VideoMeeting> meetings = videoMeetingRepository.findByIsActiveTrueAndGroupIdOrGroupIsNull(studentGroupId);

            System.out.println("🎓 Найдено встреч для студента: " + meetings.size());
            meetings.forEach(meeting -> {
                String groupInfo = meeting.getGroup() != null ?
                        "группа " + meeting.getGroup().getId() : "для всех";
                System.out.println("📝 " + meeting.getTitle() + " (" + groupInfo + ")");
            });

            return meetings.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("❌ Ошибка при получении встреч для студента: " + e.getMessage());
            e.printStackTrace();

            // Fallback: возвращаем встречи без группы
            return videoMeetingRepository.findActiveWithoutGroup().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Получает ID группы студента
     */
    private Integer getStudentGroupId(Person student) {
        try {

            if (groupService.getUserGroup(student.getUsername()) != null)
                return groupService.getUserGroup(student.getUsername());


            System.out.println("⚠️ У студента " + student.getUsername() + " не найдена группа");
            return null;

        } catch (Exception e) {
            System.err.println("❌ Ошибка получения группы студента: " + e.getMessage());
            return null;
        }
    }

    /**
     * Получает встречи созданные пользователем
     */
    public List<VideoMeetingDTO> getMeetingsByCreator(String username) {
        Person creator = peopleRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        String role = creator.getRole();

        if (role.equals("ROLE_TEACHER") || role.equals("ROLE_ADMIN")) {
            // Преподаватели видят все свои встречи
            return videoMeetingRepository.findActiveByCreatedById(creator.getId()).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            // Студенты видят только свои встречи
            return videoMeetingRepository.findByCreatedByAndIsActive(creator, true).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
    }


    public String getJoinUrl(Integer meetingId, String username, boolean isModerator) {
        VideoMeeting meeting = videoMeetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Видеовстреча не найдена"));

        return jitsiMeetService.generateJoinUrl(meeting.getMeetingId(), isModerator);
    }

    private VideoMeetingDTO convertToDTO(VideoMeeting meeting) {
        VideoMeetingDTO dto = modelMapper.map(meeting, VideoMeetingDTO.class);

        if (meeting.getCreatedBy() != null) {
            dto.setCreatedById(meeting.getCreatedBy().getId());
            dto.setCreatedByUsername(meeting.getCreatedBy().getUsername());
        }

        if (meeting.getGroup() != null) {
            dto.setGroupId(meeting.getGroup().getId());
            dto.setGroupName(meeting.getGroup().getName());
        }

        return dto;
    }

    @Transactional
    public VideoMeetingDTO updateMeeting(Integer meetingId, CreateVideoMeetingDTO updateDTO, String username) {
        VideoMeeting meeting = videoMeetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Видеовстреча не найдена"));

        Person creator = peopleRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверяем права доступа
        if (!meeting.getCreatedBy().getId().equals(creator.getId()) &&
                !creator.getRole().equals("ROLE_ADMIN")) {
            throw new RuntimeException("Нет прав для редактирования этой встречи");
        }

        LocalDateTime oldStartTime = meeting.getStartTime();

        meeting.setTitle(updateDTO.getTitle());
        meeting.setDescription(updateDTO.getDescription());
        meeting.setStartTime(updateDTO.getStartTime());
        meeting.setEndTime(updateDTO.getEndTime());
        meeting.setUpdatedAt(LocalDateTime.now());
        if (updateDTO.getStartTime() != null &&
                (oldStartTime == null || !oldStartTime.equals(updateDTO.getStartTime()))) {
            meeting.setReminderSent(false);
        }

        if (updateDTO.getGroupId() != null) {
            Optional<Group> group = groupRepository.findById(updateDTO.getGroupId());
            group.ifPresent(meeting::setGroup);
        } else {
            meeting.setGroup(null);
        }

        VideoMeeting updatedMeeting = videoMeetingRepository.save(meeting);
        return convertToDTO(updatedMeeting);
    }

    @Transactional
    public void deleteMeeting(Integer meetingId, String username) {
        VideoMeeting meeting = videoMeetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Видеовстреча не найдена"));

        Person user = peopleRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        boolean isAdmin = "ROLE_ADMIN".equals(user.getRole());
        boolean isCreatorTeacher = "ROLE_TEACHER".equals(user.getRole()) &&
                meeting.getCreatedBy() != null &&
                meeting.getCreatedBy().getId().equals(user.getId());

        if (!isAdmin && !isCreatorTeacher) {
            throw new RuntimeException("Нет прав для удаления этой встречи");
        }

        if (hasMeetingEnded(meeting) && !isAdmin) {
            throw new RuntimeException("Удалять прошедшие встречи может только администратор");
        }

        meeting.setIsActive(false);
        meeting.setUpdatedAt(LocalDateTime.now());
        videoMeetingRepository.save(meeting);
    }

    public List<VideoMeetingDTO> getAllMeetings() {
        return videoMeetingRepository.findAll().stream()
                .filter(meeting -> meeting.getIsActive() != null && meeting.getIsActive())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<VideoMeetingDTO> getMeetingsByGroup(Integer groupId) {
        return videoMeetingRepository.findActiveByGroupId(groupId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public VideoMeetingDTO getMeetingById(Integer meetingId) {
        VideoMeeting meeting = videoMeetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Видеовстреча не найдена"));
        return convertToDTO(meeting);
    }

    private boolean hasMeetingEnded(VideoMeeting meeting) {
        LocalDateTime now = LocalDateTime.now();
        if (meeting.getEndTime() != null) {
            return meeting.getEndTime().isBefore(now);
        }
        return meeting.getStartTime() != null && meeting.getStartTime().isBefore(now);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void archiveExpiredMeetings() {
        if (cleanupAfterDays <= 0) {
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusDays(cleanupAfterDays);
        List<VideoMeeting> expiredMeetings = videoMeetingRepository.findMeetingsEndedBefore(threshold);

        if (expiredMeetings.isEmpty()) {
            return;
        }

        expiredMeetings.forEach(meeting -> {
            meeting.setIsActive(false);
            meeting.setUpdatedAt(LocalDateTime.now());
        });

        videoMeetingRepository.saveAll(expiredMeetings);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void sendUpcomingMeetingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.plusMinutes(9);
        LocalDateTime windowEnd = now.plusMinutes(11);

        List<VideoMeeting> upcomingMeetings =
                videoMeetingRepository.findByIsActiveTrueAndReminderSentFalseAndStartTimeBetween(windowStart, windowEnd);

        if (upcomingMeetings.isEmpty()) {
            return;
        }

        upcomingMeetings.forEach(meeting -> {
            notifyMeetingReminder(meeting);
            meeting.setReminderSent(true);
        });

        videoMeetingRepository.saveAll(upcomingMeetings);
    }

    private void notifyMeetingCreated(VideoMeeting meeting) {
        List<Person> recipients = resolveMeetingParticipants(meeting);
        String groupName = meeting.getGroup() != null ? meeting.getGroup().getName() : null;

        recipients.forEach(person -> notificationProducerService.sendVideoMeetingCreatedNotification(
                person.getId(),
                person.getRole(),
                meeting.getTitle(),
                meeting.getStartTime(),
                meeting.getId(),
                groupName
        ));
    }

    private void notifyMeetingReminder(VideoMeeting meeting) {
        List<Person> recipients = resolveMeetingParticipants(meeting);

        recipients.forEach(person -> notificationProducerService.sendVideoMeetingReminderNotification(
                person.getId(),
                person.getRole(),
                meeting.getTitle(),
                meeting.getStartTime(),
                meeting.getId(),
                meeting.getMeetingUrl()
        ));
    }

    private List<Person> resolveMeetingParticipants(VideoMeeting meeting) {
        Set<Integer> seenIds = new HashSet<>();
        List<Person> participants = new ArrayList<>();

        Integer groupId = meeting.getGroup() != null ? meeting.getGroup().getId() : null;
        if (groupId != null) {
            participants.addAll(groupService.getPersonsByGroupId(groupId));
        } else {
            participants.addAll(peopleRepository.findByRole("ROLE_STUDENT"));
        }

        if (meeting.getCreatedBy() != null) {
            participants.add(meeting.getCreatedBy());
        }

        return participants.stream()
                .filter(person -> person != null && person.getId() != null)
                .filter(person -> seenIds.add(person.getId()))
                .collect(Collectors.toList());
    }
}


