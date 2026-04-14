package ru.danon.spring.ToDo.services;

import java.util.Map;

public interface JitsiMeetService {
    Map<String, String> createMeeting(String title, String description);

    String generateJoinUrl(String meetingId, boolean isModerator);
}
