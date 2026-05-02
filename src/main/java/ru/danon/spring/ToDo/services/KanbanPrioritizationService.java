package ru.danon.spring.ToDo.services;

import java.time.LocalDate;

public interface KanbanPrioritizationService {
    int calculateScore(LocalDate start, LocalDate deadline, String priority, boolean optimizedBefore);
}
