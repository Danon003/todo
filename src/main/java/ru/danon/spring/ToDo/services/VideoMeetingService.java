package ru.danon.spring.ToDo.services;

import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.CreateVideoMeetingDTO;
import ru.danon.spring.ToDo.dto.VideoMeetingDTO;

import java.util.List;

public interface VideoMeetingService {
    @Transactional
    VideoMeetingDTO createMeeting(CreateVideoMeetingDTO createDTO, String username);

    List<VideoMeetingDTO> getAllMeetings(Authentication authentication);

    List<VideoMeetingDTO> getMeetingsForStudent(String username);

    List<VideoMeetingDTO> getMeetingsByCreator(String username);

    String getJoinUrl(Integer meetingId, String username, boolean isModerator);

    @Transactional
    VideoMeetingDTO updateMeeting(Integer meetingId, CreateVideoMeetingDTO updateDTO, String username);

    @Transactional
    void deleteMeeting(Integer meetingId, String username);

    List<VideoMeetingDTO> getAllMeetings();

    List<VideoMeetingDTO> getMeetingsByGroup(Integer groupId);

    VideoMeetingDTO getMeetingById(Integer meetingId);

    @Transactional
    void archiveExpiredMeetings();

    @Transactional
    void sendUpcomingMeetingReminders();

    @Transactional
    void completeMeeting(Integer meetingId, String username);
}
