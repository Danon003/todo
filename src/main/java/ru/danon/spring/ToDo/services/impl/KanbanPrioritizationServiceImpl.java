package ru.danon.spring.ToDo.services.impl;

import org.springframework.stereotype.Service;
import ru.danon.spring.ToDo.services.KanbanPrioritizationService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class KanbanPrioritizationServiceImpl implements KanbanPrioritizationService {

    @Override
    public int calculateScore(LocalDate start, LocalDate deadline, String priority, boolean optimizedBefore) {
        int urgencyScore = deadline == null
                ? 10
                : Math.max(0, 100 - (int) (ChronoUnit.DAYS.between(start, deadline) * 7));

        int importanceScore = switch (priority == null ? "MEDIUM" : priority.toUpperCase()) {
            case "HIGH" -> 100;
            case "MEDIUM" -> 60;
            case "LOW" -> 30;
            default -> 50;
        };

        int eisenhowerBoost = (urgencyScore >= 70 && importanceScore >= 70) ? 25
                : (urgencyScore < 40 && importanceScore < 40) ? -10
                : 5;

        int stabilityPenalty = optimizedBefore ? 5 : 0;
        return urgencyScore + importanceScore + eisenhowerBoost - stabilityPenalty;
    }
}
