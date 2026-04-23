package ru.danon.spring.ToDo.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.services.NotificationProcessingService;
import ru.danon.spring.ToDo.services.TaskService;
import ru.danon.spring.ToDo.services.VideoMeetingService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final VideoMeetingService videoMeetingServiceImpl;
    private final TaskService taskServiceImpl;
    private final NotificationProcessingService notificationProcessingServiceImpl;

    /**
     * Архивация встреч старше 4 дней
     * Выполняется каждый день в 4:00 утра
     */
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void archiveExpiredMeetings() {
        log.info("Запуск плановой архивации просроченных видеовстреч");
        try {
            videoMeetingServiceImpl.archiveExpiredMeetings();
            log.info("Плановая архивация видеовстреч успешно завершена");
        } catch (Exception e) {
            log.error("Ошибка при плановой архивации видеовстреч: {}", e.getMessage(), e);
        }
    }

    /**
     * Отправка напоминаний о предстоящих встречах
     * Выполняется каждые 10 минут (600000 мс)
     */
    @Scheduled(fixedDelay = 600000)
    @Transactional
    public void sendMeetingReminders() {
        log.debug("Запуск проверки напоминаний о предстоящих видеовстречах");
        try {
            videoMeetingServiceImpl.sendUpcomingMeetingReminders();
        } catch (Exception e) {
            log.error("Ошибка при отправке напоминаний о видеовстречах: {}", e.getMessage(), e);
        }
    }

    /**
     * Обновление статусов просроченных задач
     * Выполняется каждые 2 минуты (120000 мс)
     */
    @Scheduled(fixedDelay = 120000)
    @Transactional
    public void updateOverdueTasks() {
        log.debug("Запуск проверки просроченных задач");
        try {
            taskServiceImpl.updateOverdueTasks();
        } catch (Exception e) {
            log.error("Ошибка при обновлении статусов просроченных задач: {}", e.getMessage(), e);
        }
    }

    /**
     * Обработка запланированных уведомлений
     * Выполняется каждые 5 минут (300000 мс)
     */
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void processScheduledNotifications() {
        log.debug("Запуск обработки запланированных уведомлений");
        try {
            notificationProcessingServiceImpl.processScheduledNotifications();
        } catch (Exception e) {
            log.error("Ошибка при обработке запланированных уведомлений: {}", e.getMessage(), e);
        }
    }
}
