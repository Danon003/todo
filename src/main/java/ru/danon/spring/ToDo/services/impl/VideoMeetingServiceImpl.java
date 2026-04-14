package ru.danon.spring.ToDo.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.CreateVideoMeetingDTO;
import ru.danon.spring.ToDo.dto.VideoMeetingDTO;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.models.postgre.Group;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.models.postgre.VideoMeeting;
import ru.danon.spring.ToDo.repositories.jpa.GroupRepository;
import ru.danon.spring.ToDo.repositories.jpa.PeopleRepository;
import ru.danon.spring.ToDo.repositories.jpa.VideoMeetingRepository;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.JitsiMeetService;
import ru.danon.spring.ToDo.services.NotificationProducerService;
import ru.danon.spring.ToDo.services.VideoMeetingService;

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
@Slf4j
public class VideoMeetingServiceImpl implements VideoMeetingService {

    private final VideoMeetingRepository videoMeetingRepository;
    private final PeopleRepository peopleRepository;
    private final GroupRepository groupRepository;
    private final JitsiMeetService jitsiMeetServiceImpl;
    private final ModelMapper modelMapper;
    private final GroupService groupServiceImpl;
    private final NotificationProducerService notificationProducerServiceImpl;
    private final long cleanupAfterDays;

    @Autowired
    public VideoMeetingServiceImpl(
            VideoMeetingRepository videoMeetingRepository,
            PeopleRepository peopleRepository,
            GroupRepository groupRepository,
            JitsiMeetService jitsiMeetServiceImpl,
            ModelMapper modelMapper,
            GroupService groupServiceImpl,
            NotificationProducerService notificationProducerServiceImpl,
            @Value("${video.meetings.cleanup-after-days:4}") long cleanupAfterDays) {
        this.videoMeetingRepository = videoMeetingRepository;
        this.peopleRepository = peopleRepository;
        this.groupRepository = groupRepository;
        this.jitsiMeetServiceImpl = jitsiMeetServiceImpl;
        this.modelMapper = modelMapper;
        this.groupServiceImpl = groupServiceImpl;
        this.notificationProducerServiceImpl = notificationProducerServiceImpl;
        this.cleanupAfterDays = cleanupAfterDays;
    }

    @Transactional @Override public VideoMeetingDTO createMeeting(CreateVideoMeetingDTO createDTO, String username) {
        log.info("Создание видеовстречи пользователем: {}", username);

        Person creator = peopleRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Пользователь {} не найден", username);
                    return new EntityNotFoundException("Пользователь не найден", username);
                });

        if (!"ROLE_TEACHER".equals(creator.getRole())) {
            log.warn("Пользователь {} с ролью {} пытается создать видеовстречу", username, creator.getRole());
            throw new AccessDeniedException("Назначать видеовстречи может только преподаватель");
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

        Map<String, String> meetResult = jitsiMeetServiceImpl.createMeeting(
                createDTO.getTitle(),
                createDTO.getDescription()
        );
        meeting.setMeetingUrl(meetResult.get("meetingUrl"));
        meeting.setMeetingId(meetResult.get("meetingId"));

        VideoMeeting savedMeeting = videoMeetingRepository.save(meeting);
        log.info("Видеовстреча создана: id={}, title={}, groupId={}", savedMeeting.getId(), savedMeeting.getTitle(), createDTO.getGroupId());

        notifyMeetingCreated(savedMeeting);
        return convertToDTO(savedMeeting);
    }

    /**
     * Получает все встречи с учетом роли пользователя
     */
    @Override public List<VideoMeetingDTO> getAllMeetings(Authentication authentication) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        log.debug("Получение всех встреч для пользователя: {}, роль: {}", username, role);

        Person user = peopleRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Пользователь {} не найден", username);
                    return new RuntimeException("Пользователь не найден");
                });

        List<VideoMeetingDTO> meetings;
        if (role.equals("ROLE_TEACHER") || role.equals("ROLE_ADMIN")) {
            // Преподаватели видят все активные встречи
            meetings = videoMeetingRepository.findAll().stream()
                    .filter(meeting -> meeting.getIsActive() != null && meeting.getIsActive())
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            log.debug("Преподаватель/админ {} видит {} встреч", username, meetings.size());
        } else {
            // Студенты видят встречи своей группы ИЛИ встречи без группы
            meetings = getMeetingsForStudent(user.getUsername());
        }

        return meetings;
    }

    /**
     * Получает встречи доступные студенту
     * - встречи без группы (для всех)
     * - встречи группы студента
     */
    @Override public List<VideoMeetingDTO> getMeetingsForStudent(String username) {
        log.debug("Получение встреч для студента: {}", username);

        try {
            Person student = peopleRepository.findByUsername(username)
                    .orElseThrow(() -> new EntityNotFoundException("Студент не найден: ", username));

            Integer studentGroupId = getStudentGroupId(student);

            List<VideoMeeting> meetings = videoMeetingRepository.findByIsActiveTrueAndGroupIdOrGroupIsNull(studentGroupId);
            log.debug("Студент {} видит {} встреч (groupId={})", username, meetings.size(), studentGroupId);

            return meetings.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Ошибка при получении встреч для студента {}: {}", username, e.getMessage());

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
            Integer groupId = groupServiceImpl.getUserGroup(student.getUsername());
            return groupId;
        } catch (Exception e) {
            log.error("Ошибка получения группы студента {}: {}", student.getUsername(), e.getMessage());
            return null;
        }
    }

    /**
     * Получает встречи созданные пользователем
     */
    @Override public List<VideoMeetingDTO> getMeetingsByCreator(String username) {
        log.debug("Получение встреч созданных пользователем: {}", username);

        Person creator = peopleRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Пользователь {} не найден", username);
                    return new RuntimeException("Пользователь не найден");
                });

        String role = creator.getRole();

        List<VideoMeetingDTO> meetings;
        if (role.equals("ROLE_TEACHER") || role.equals("ROLE_ADMIN")) {
            meetings = videoMeetingRepository.findActiveByCreatedById(creator.getId()).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            meetings = videoMeetingRepository.findByCreatedByAndIsActive(creator, true).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }

        log.debug("Найдено {} встреч созданных пользователем {}", meetings.size(), username);
        return meetings;
    }


    @Override public String getJoinUrl(Integer meetingId, String username, boolean isModerator) {
        log.debug("Получение ссылки для присоединения к встрече id={}, пользователь: {}, модератор: {}", meetingId, username, isModerator);

        VideoMeeting meeting = videoMeetingRepository.findById(meetingId)
                .orElseThrow(() -> {
                    log.error("Видеовстреча id={} не найдена", meetingId);
                    return new EntityNotFoundException("Видеовстреча не найдена", meetingId);
                });

        return jitsiMeetServiceImpl.generateJoinUrl(meeting.getMeetingId(), isModerator);
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

    @Transactional @Override public VideoMeetingDTO updateMeeting(Integer meetingId, CreateVideoMeetingDTO updateDTO, String username) {
        log.info("Обновление видеовстречи id={} пользователем: {}", meetingId, username);

        VideoMeeting meeting = videoMeetingRepository.findById(meetingId)
                .orElseThrow(() -> {
                    log.error("Видеовстреча id={} не найдена", meetingId);
                    return new EntityNotFoundException("Видеовстреча не найдена", meetingId);
                });

        Person creator = peopleRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Пользователь {} не найден", username);
                    return new EntityNotFoundException("Пользователь не найден", username);
                });

        if (!meeting.getCreatedBy().getId().equals(creator.getId()) &&
                !creator.getRole().equals("ROLE_ADMIN")) {
            log.warn("Пользователь {} пытается редактировать чужую встречу id={}", username, meetingId);
            throw new AccessDeniedException("Нет прав для редактирования этой встречи");
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
        log.info("Видеовстреча id={} успешно обновлена", meetingId);
        return convertToDTO(updatedMeeting);
    }

    @Transactional @Override public void deleteMeeting(Integer meetingId, String username) {
        log.info("Удаление видеовстречи id={} пользователем: {}", meetingId, username);

        VideoMeeting meeting = videoMeetingRepository.findById(meetingId)
                .orElseThrow(() -> {
                    log.error("Видеовстреча id={} не найдена", meetingId);
                    return new EntityNotFoundException("Видеовстреча не найдена", meetingId);
                });

        Person user = peopleRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Пользователь {} не найден", username);
                    return new EntityNotFoundException("Пользователь не найден", username);
                });

        boolean isCreatorTeacher = "ROLE_TEACHER".equals(user.getRole()) &&
                meeting.getCreatedBy() != null &&
                meeting.getCreatedBy().getId().equals(user.getId());

        if (!isCreatorTeacher) {
            log.warn("Пользователь {} пытается удалить чужую встречу id={}", username, meetingId);
            throw new AccessDeniedException("Нет прав для удаления этой встречи");
        }

        meeting.setIsActive(false);
        meeting.setUpdatedAt(LocalDateTime.now());
        videoMeetingRepository.save(meeting);
        log.info("Видеовстреча id={} помечена как неактивная", meetingId);
    }

    @Override public List<VideoMeetingDTO> getAllMeetings() {
        log.debug("Получение всех активных встреч");
        List<VideoMeetingDTO> meetings = videoMeetingRepository.findAll().stream()
                .filter(meeting -> meeting.getIsActive() != null && meeting.getIsActive())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.debug("Найдено {} активных встреч", meetings.size());
        return meetings;
    }

    @Override public List<VideoMeetingDTO> getMeetingsByGroup(Integer groupId) {
        log.debug("Получение встреч для группы id={}", groupId);
        List<VideoMeetingDTO> meetings = videoMeetingRepository.findActiveByGroupId(groupId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.debug("Найдено {} встреч для группы id={}", meetings.size(), groupId);
        return meetings;
    }

    @Override public VideoMeetingDTO getMeetingById(Integer meetingId) {
        log.debug("Получение встречи по id={}", meetingId);
        VideoMeeting meeting = videoMeetingRepository.findById(meetingId)
                .orElseThrow(() -> {
                    log.error("Видеовстреча id={} не найдена", meetingId);
                    return new EntityNotFoundException("Видеовстреча не найдена", meetingId);
                });
        return convertToDTO(meeting);
    }

    private boolean hasMeetingEnded(VideoMeeting meeting) {
        LocalDateTime now = LocalDateTime.now();
        if (meeting.getEndTime() != null) {
            return meeting.getEndTime().isBefore(now);
        }
        return meeting.getStartTime() != null && meeting.getStartTime().isBefore(now);
    }

    @Transactional @Override public void archiveExpiredMeetings() {
        if (cleanupAfterDays <= 0) {
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusDays(cleanupAfterDays);
        List<VideoMeeting> expiredMeetings = videoMeetingRepository.findMeetingsEndedBefore(threshold);

        if (expiredMeetings.isEmpty()) {
            return;
        }

        log.info("Архивация {} просроченных встреч (старше {} дней)", expiredMeetings.size(), cleanupAfterDays);

        expiredMeetings.forEach(meeting -> {
            meeting.setIsActive(false);
            meeting.setUpdatedAt(LocalDateTime.now());
        });

        videoMeetingRepository.saveAll(expiredMeetings);
        log.info("Архивировано {} встреч", expiredMeetings.size());
    }


    @Transactional @Override public void sendUpcomingMeetingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.plusMinutes(9);
        LocalDateTime windowEnd = now.plusMinutes(11);

        List<VideoMeeting> upcomingMeetings =
                videoMeetingRepository.findByIsActiveTrueAndReminderSentFalseAndStartTimeBetween(windowStart, windowEnd);

        if (upcomingMeetings.isEmpty()) {
            return;
        }

        log.info("Отправка напоминаний о {} предстоящих встречах", upcomingMeetings.size());

        upcomingMeetings.forEach(meeting -> {
            notifyMeetingReminder(meeting);
            meeting.setReminderSent(true);
        });

        videoMeetingRepository.saveAll(upcomingMeetings);
    }

    private void notifyMeetingCreated(VideoMeeting meeting) {
        List<Person> recipients = resolveMeetingParticipants(meeting);
        String groupName = meeting.getGroup() != null ? meeting.getGroup().getName() : "всех студентов";

        log.debug("Отправка уведомлений о создании встречи id={} {} получателям", meeting.getId(), recipients.size());

        recipients.forEach(person -> notificationProducerServiceImpl.sendVideoMeetingCreatedNotification(
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

        log.debug("Отправка напоминаний о встрече id={} {} получателям", meeting.getId(), recipients.size());

        recipients.forEach(person -> notificationProducerServiceImpl.sendVideoMeetingReminderNotification(
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
            participants.addAll(groupServiceImpl.getPersonsByGroupId(groupId));
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

    @Transactional @Override public void completeMeeting(Integer meetingId, String username) {
        log.info("Завершение встречи id={} пользователем: {}", meetingId, username);

        VideoMeeting meeting = videoMeetingRepository.findById(meetingId)
                .orElseThrow(() -> {
                    log.error("Встреча id={} не найдена", meetingId);
                    return new EntityNotFoundException("Встреча не найдена", meetingId);
                });

        Person user = peopleRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Пользователь {} не найден", username);
                    return new EntityNotFoundException("Пользователь не найден", username);
                });

        // Проверяем права: создатель встречи или админ
        if (!meeting.getCreatedBy().getId().equals(user.getId()) &&
                !user.getRole().equals("ROLE_ADMIN")) {
            log.warn("Пользователь {} пытается завершить чужую встречу id={}", username, meetingId);
            throw new AccessDeniedException("Вы можете завершать только свои встречи");
        }

        // Проверяем, что встреча активна
        if (!meeting.getIsActive()) {
            log.warn("Попытка завершить уже неактивную встречу id={}", meetingId);
            throw new RuntimeException("Встреча уже завершена");
        }

        // Завершаем встречу
        meeting.setIsActive(false);
        meeting.setUpdatedAt(LocalDateTime.now());

        videoMeetingRepository.save(meeting);
        log.info("Встреча id={} успешно завершена", meetingId);
    }
}
