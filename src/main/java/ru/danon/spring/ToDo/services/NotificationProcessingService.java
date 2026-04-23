package ru.danon.spring.ToDo.services;

import org.springframework.transaction.annotation.Transactional;

public interface NotificationProcessingService {
    @Transactional
    void processScheduledNotifications();
}
