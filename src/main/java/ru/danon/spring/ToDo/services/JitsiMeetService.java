package ru.danon.spring.ToDo.services;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JitsiMeetService {

    /**
     * Создает Jitsi Meet встречу
     * Полностью бесплатно, без ограничений по времени!
     */
    public Map<String, String> createMeeting(String title, String description) {
        Map<String, String> result = new HashMap<>();

        // Генерируем человеко-читаемый ID встречи
        String meetingId = generateMeetingId(title);
        String meetingUrl = "https://meet.jit.si/" + meetingId;

        result.put("meetingUrl", meetingUrl);
        result.put("meetingId", meetingId);

        System.out.println("🎉 Создана Jitsi Meet встреча:");
        System.out.println("📝 Название: " + title);
        System.out.println("🔗 Ссылка: " + meetingUrl);
        System.out.println("🆔 ID: " + meetingId);

        return result;
    }

    /**
     * Генерирует уникальный ID встречи на основе названия
     */
    private String generateMeetingId(String title) {
        // Очищаем название от спецсимволов
        String cleanTitle = title.toLowerCase()
                .replaceAll("[^a-z0-9а-яё\\-]", "-")  // Оставляем только буквы, цифры и дефисы
                .replaceAll("-+", "-")                 // Заменяем multiple дефисы на один
                .replaceAll("^-|-$", "")              // Убираем дефисы в начале и конце
                .trim();

        // Если после очистки строка пустая, используем случайный ID
        if (cleanTitle.isEmpty()) {
            cleanTitle = "meeting";
        }

        // Ограничиваем длину и добавляем случайную часть
        if (cleanTitle.length() > 30) {
            cleanTitle = cleanTitle.substring(0, 30);
        }

        // Добавляем случайную часть для уникальности
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);

        return cleanTitle + "-" + randomSuffix;
    }

    /**
     * Генерирует ссылку для присоединения к встрече
     */
    public String generateJoinUrl(String meetingId, boolean isModerator) {
        // В Jitsi Meet все участники равны, но можно добавить параметры
        String joinUrl = "https://meet.jit.si/" + meetingId;

        // Опционально: добавляем параметры для автоматического подключения
        if (isModerator) {
            joinUrl += "#config.startWithAudioMuted=true&config.startWithVideoMuted=false";
        } else {
            joinUrl += "#config.startWithAudioMuted=true&config.startWithVideoMuted=true";
        }

        return joinUrl;
    }
}