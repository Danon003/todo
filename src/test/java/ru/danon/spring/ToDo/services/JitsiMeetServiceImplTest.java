package ru.danon.spring.ToDo.services;

import org.junit.jupiter.api.Test;
import ru.danon.spring.ToDo.services.impl.JitsiMeetServiceImpl;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JitsiMeetServiceImplTest {

    private final JitsiMeetServiceImpl service = new JitsiMeetServiceImpl();

    @Test
    void createMeetingShouldGenerateUrlAndId() {
        Map<String, String> result = service.createMeeting("Java Group", "weekly");

        assertNotNull(result.get("meetingId"));
        assertTrue(result.get("meetingUrl").startsWith("https://meet.jit.si/"));
        assertTrue(result.get("meetingUrl").contains(result.get("meetingId")));
    }

    @Test
    void createMeetingShouldFallbackToMeetingWhenTitleInvalid() {
        Map<String, String> result = service.createMeeting("!!!", "desc");

        assertTrue(result.get("meetingId").startsWith("meeting-"));
    }

    @Test
    void generateJoinUrlShouldSetModeratorParams() {
        String url = service.generateJoinUrl("room-1", true);

        assertTrue(url.contains("room-1"));
        assertTrue(url.contains("startWithVideoMuted=false"));
    }
}
