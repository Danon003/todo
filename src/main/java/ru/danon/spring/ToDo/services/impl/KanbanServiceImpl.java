package ru.danon.spring.ToDo.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.kanban.KanbanBoardResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanDayDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanApplyOptimizationRequestDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanApplyOptimizationResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanItemDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanInsightsResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanMoveRequestDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanCoachingInsightDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanOptimizationResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanOptimizationSuggestionDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanWeeklyPlanDayDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanWeeklyPlanResponseDTO;
import ru.danon.spring.ToDo.dto.kanban.KanbanWhatIfResponseDTO;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.models.postgre.KanbanTask;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.models.postgre.TaskAssignment;
import ru.danon.spring.ToDo.models.postgre.VideoMeeting;
import ru.danon.spring.ToDo.repositories.jpa.KanbanTaskRepository;
import ru.danon.spring.ToDo.repositories.jpa.PeopleRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskAssignmentRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskRepository;
import ru.danon.spring.ToDo.repositories.jpa.VideoMeetingRepository;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.KanbanPrioritizationService;
import ru.danon.spring.ToDo.services.KanbanService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class KanbanServiceImpl implements KanbanService {
    private static final int DEFAULT_DAYS = 14;
    private static final int HISTORY_DAYS = 28;

    private final KanbanTaskRepository kanbanTaskRepository;
    private final PeopleRepository peopleRepository;
    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final VideoMeetingRepository videoMeetingRepository;
    private final GroupService groupService;
    private final KanbanPrioritizationService kanbanPrioritizationService;

    @Override
    @Transactional
    @Cacheable(cacheNames = "kanbanBoard", key = "#username + ':' + #startDate + ':' + #endDate")
    public KanbanBoardResponseDTO getBoard(String username, LocalDate startDate, LocalDate endDate) {
        Person user = getUser(username);
        LocalDate from = startDate == null ? LocalDate.now() : startDate;
        LocalDate to = endDate == null ? from.plusDays(DEFAULT_DAYS - 1) : endDate;
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("endDate не может быть раньше startDate");
        }

        syncBoardItems(user, from, to);

        List<KanbanTask> rows = kanbanTaskRepository
                .findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscPositionAsc(user.getId(), from, to);

        Map<LocalDate, List<KanbanItemDTO>> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        KanbanTask::getScheduledDate,
                        HashMap::new,
                        Collectors.mapping(this::toItemDto, Collectors.toList())
                ));

        List<KanbanDayDTO> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            days.add(KanbanDayDTO.builder()
                    .date(date)
                    .items(grouped.getOrDefault(date, List.of()))
                    .build());
        }

        return KanbanBoardResponseDTO.builder()
                .startDate(from)
                .endDate(to)
                .days(days)
                .totalItems(rows.size())
                .totalFixedItems((int) rows.stream().filter(item -> Boolean.TRUE.equals(item.getIsFixed())).count())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "kanbanBoard", allEntries = true)
    public KanbanItemDTO move(Long kanbanTaskId, KanbanMoveRequestDTO request, String username) {
        Person user = getUser(username);
        KanbanTask card = kanbanTaskRepository.findByIdAndUserId(kanbanTaskId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Kanban task not found", kanbanTaskId));

        if (Boolean.TRUE.equals(card.getIsFixed())) {
            throw new IllegalArgumentException("Фиксированные события нельзя перемещать");
        }

        LocalDate targetDate = request.getScheduledDate();
        if (targetDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Нельзя переместить задачу на прошедшую дату");
        }

        LocalDate limitDate = resolveLimitDate(card);
        if (limitDate != null && targetDate.isAfter(limitDate)) {
            throw new IllegalArgumentException("Нельзя перемещать задачу позже дедлайна");
        }

        LocalDate oldDate = card.getScheduledDate();
        int requestedPosition = Math.max(0, request.getPosition());

        List<KanbanTask> oldDay = kanbanTaskRepository.findByUserIdAndScheduledDateOrderByPositionAsc(user.getId(), oldDate);
        oldDay.removeIf(item -> Objects.equals(item.getId(), card.getId()));
        reindex(oldDay);
        kanbanTaskRepository.saveAll(oldDay);

        List<KanbanTask> newDay = kanbanTaskRepository.findByUserIdAndScheduledDateOrderByPositionAsc(user.getId(), targetDate);
        int finalPosition = Math.min(requestedPosition, newDay.size());
        newDay.add(finalPosition, card);

        card.setScheduledDate(targetDate);
        card.setPosition(finalPosition);
        card.setIsOptimized(false);
        reindex(newDay);
        kanbanTaskRepository.saveAll(newDay);

        return toItemDto(card);
    }

    @Override
    @Transactional
    public KanbanOptimizationResponseDTO optimize(String username, int dailyLimit, int bufferDays) {
        Person user = getUser(username);
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(DEFAULT_DAYS - 1);
        int baseDailyLimit = Math.max(1, dailyLimit);
        int buffer = Math.max(1, Math.min(2, bufferDays));

        syncBoardItems(user, start, end);

        List<KanbanTask> allCards = kanbanTaskRepository
                .findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscPositionAsc(user.getId(), start, end)
                .stream().toList();
        Map<Long, TaskAssignment> assignmentByTaskId = taskAssignmentRepository.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(TaskAssignment::getTaskId, a -> a, (a, b) -> a));
        List<KanbanTask> cards = allCards.stream().filter(card -> !Boolean.TRUE.equals(card.getIsFixed())).toList();
        int historyCapacity = estimateHistoricalDailyCapacity(user.getId());
        int effectiveDailyLimit = Math.max(1, Math.min(8, Math.max(baseDailyLimit, historyCapacity)));
        Map<LocalDate, Integer> fixedLoad = buildFixedLoadMap(allCards, start, end);

        List<PlanningCandidate> sorted = cards.stream()
                .map(card -> toCandidate(card, start, assignmentByTaskId))
                .sorted(Comparator
                        .comparingInt(PlanningCandidate::score).reversed()
                        .thenComparing(PlanningCandidate::deadline, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(candidate -> candidate.card().getId()))
                .toList();

        Map<LocalDate, Integer> dayLoad = new HashMap<>();
        List<KanbanOptimizationSuggestionDTO> suggestions = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        int riskTasks = 0;
        Set<LocalDate> overloadedDays = new java.util.HashSet<>();

        for (PlanningCandidate candidate : sorted) {
            KanbanTask card = candidate.card();
            LocalDate currentDate = card.getScheduledDate();
            LocalDate deadline = candidate.deadline();
            int dynamicBuffer = candidate.priorityWeight() <= 1 ? 2 : buffer;
            LocalDate hardLimit = deadline == null ? end : deadline.minusDays(dynamicBuffer);
            if (hardLimit.isBefore(start)) {
                hardLimit = start;
            }

            LocalDate suggestedDate = null;
            for (LocalDate d = start; !d.isAfter(hardLimit); d = d.plusDays(1)) {
                int maxForDay = Math.max(1, effectiveDailyLimit - fixedLoad.getOrDefault(d, 0));
                if (dayLoad.getOrDefault(d, 0) < maxForDay) {
                    suggestedDate = d;
                    break;
                }
            }

            String riskLevel = "LOW";
            if (suggestedDate == null) {
                suggestedDate = hardLimit;
                riskLevel = "HIGH";
                riskTasks++;
                recommendations.add("Есть риск срыва дедлайна для карточки #" + card.getId() + ", нужен перенос нагрузки");
            } else if (deadline != null && !suggestedDate.isBefore(deadline.minusDays(1))) {
                riskLevel = "MEDIUM";
                riskTasks++;
            }

            int suggestedPosition = dayLoad.getOrDefault(suggestedDate, 0);
            dayLoad.put(suggestedDate, suggestedPosition + 1);
            int maxForSuggestedDay = Math.max(1, effectiveDailyLimit - fixedLoad.getOrDefault(suggestedDate, 0));
            if (dayLoad.get(suggestedDate) > maxForSuggestedDay) {
                overloadedDays.add(suggestedDate);
            }

            if (!suggestedDate.equals(currentDate) || card.getPosition() != suggestedPosition) {
                suggestions.add(KanbanOptimizationSuggestionDTO.builder()
                        .kanbanTaskId(card.getId())
                        .currentDate(currentDate)
                        .suggestedDate(suggestedDate)
                        .suggestedPosition(suggestedPosition)
                        .reason(buildReason(deadline, resolvePriority(card, assignmentByTaskId), dynamicBuffer))
                        .riskLevel(riskLevel)
                        .score(candidate.score())
                        .build());
            }
        }

        KanbanCoachingInsightDTO coaching = buildCoachingInsight(user.getId(), effectiveDailyLimit);
        if (recommendations.isEmpty() && !suggestions.isEmpty()) {
            recommendations.add("Рекомендуется принять оптимизацию: график выровнен по дням с учётом приоритетов и буфера");
        }
        if (suggestions.isEmpty()) {
            recommendations.add("График уже выглядит оптимально на ближайшие две недели");
        }
        if (overloadedDays.size() > 2) {
            recommendations.add("На горизонте 2 недель обнаружено много перегруженных дней, лучше снизить ежедневный лимит задач");
        }
        if ("HIGH".equals(coaching.getBurnoutRisk())) {
            recommendations.add("Есть признаки перегрузки: рассмотрите делегирование/перенос части задач");
        }

        return KanbanOptimizationResponseDTO.builder()
                .suggestions(suggestions)
                .recommendations(recommendations.stream().distinct().toList())
                .effectiveDailyLimit(effectiveDailyLimit)
                .overloadedDays(overloadedDays.size())
                .riskTasksCount(riskTasks)
                .coaching(coaching)
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "kanbanBoard", allEntries = true)
    public KanbanApplyOptimizationResponseDTO applyOptimization(String username, KanbanApplyOptimizationRequestDTO request) {
        Person user = getUser(username);
        List<KanbanItemDTO> updatedItems = new ArrayList<>();

        for (var item : request.getItems()) {
            KanbanMoveRequestDTO moveRequest = new KanbanMoveRequestDTO();
            moveRequest.setScheduledDate(item.getSuggestedDate());
            moveRequest.setPosition(item.getSuggestedPosition());
            KanbanItemDTO updated = move(item.getKanbanTaskId(), moveRequest, username);

            KanbanTask entity = kanbanTaskRepository.findByIdAndUserId(item.getKanbanTaskId(), user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Kanban task not found", item.getKanbanTaskId()));
            entity.setIsOptimized(true);
            kanbanTaskRepository.save(entity);
            updatedItems.add(updated);
        }

        return KanbanApplyOptimizationResponseDTO.builder()
                .appliedCount(updatedItems.size())
                .updatedItems(updatedItems)
                .build();
    }

    @Override
    @Transactional
    public KanbanInsightsResponseDTO getInsights(String username, LocalDate startDate, LocalDate endDate) {
        Person user = getUser(username);
        LocalDate from = startDate == null ? LocalDate.now() : startDate;
        LocalDate to = endDate == null ? from.plusDays(DEFAULT_DAYS - 1) : endDate;
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("endDate не может быть раньше startDate");
        }

        syncBoardItems(user, from, to);
        List<KanbanTask> cards = kanbanTaskRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscPositionAsc(
                user.getId(), from, to
        );
        List<KanbanTask> movable = cards.stream().filter(card -> !Boolean.TRUE.equals(card.getIsFixed())).toList();
        int capacity = Math.max(1, estimateHistoricalDailyCapacity(user.getId()));
        Map<LocalDate, Long> tasksPerDay = movable.stream().collect(Collectors.groupingBy(
                KanbanTask::getScheduledDate, Collectors.counting()
        ));
        int overloadedDays = (int) tasksPerDay.values().stream().filter(count -> count > capacity).count();
        int riskTasks = (int) movable.stream()
                .filter(card -> {
                    LocalDate deadline = resolveLimitDate(card);
                    return deadline != null && !card.getScheduledDate().isBefore(deadline.minusDays(1));
                }).count();

        KanbanCoachingInsightDTO coaching = buildCoachingInsight(user.getId(), capacity);
        List<String> recommendations = new ArrayList<>();
        if (riskTasks > 0) {
            recommendations.add("Есть задачи, запланированные почти в дедлайн. Лучше переносить их минимум за 1-2 дня");
        }
        if (overloadedDays > 0) {
            recommendations.add("Обнаружены перегруженные дни. Распределите задачи более равномерно по неделе");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("План выглядит сбалансированным. Поддерживайте текущий темп");
        }
        recommendations.add(coaching.getAdvice());

        return KanbanInsightsResponseDTO.builder()
                .startDate(from)
                .endDate(to)
                .totalPlanned(cards.size())
                .overloadedDays(overloadedDays)
                .riskTasksCount(riskTasks)
                .recommendations(recommendations.stream().distinct().toList())
                .coaching(coaching)
                .build();
    }

    @Override
    @Transactional
    public KanbanWhatIfResponseDTO whatIf(String username, Integer minDailyLimit, Integer maxDailyLimit, Integer minBuffer, Integer maxBuffer) {
        int minLimit = Math.max(1, minDailyLimit == null ? 2 : minDailyLimit);
        int maxLimit = Math.max(minLimit, maxDailyLimit == null ? 5 : maxDailyLimit);
        int fromBuffer = Math.max(1, minBuffer == null ? 1 : minBuffer);
        int toBuffer = Math.max(fromBuffer, maxBuffer == null ? 2 : maxBuffer);

        List<ScenarioCandidate> scenarios = new ArrayList<>();
        for (int limit = minLimit; limit <= maxLimit; limit++) {
            for (int buffer = fromBuffer; buffer <= toBuffer; buffer++) {
                KanbanOptimizationResponseDTO result = optimize(username, limit, buffer);
                int score = scenarioScore(result);
                scenarios.add(new ScenarioCandidate(limit, buffer, score, result));
            }
        }

        scenarios.sort(Comparator.comparingInt(ScenarioCandidate::score));
        ScenarioCandidate best = scenarios.getFirst();
        return KanbanWhatIfResponseDTO.builder()
                .scenarios(scenarios.stream().map(ScenarioCandidate::result).toList())
                .bestScenarioDailyLimit(best.dailyLimit())
                .bestScenarioBufferDays(best.bufferDays())
                .bestScenarioReason("Минимум рисков (" + best.result().getRiskTasksCount() +
                        "), перегруженных дней (" + best.result().getOverloadedDays() + ") и высокий баланс нагрузки")
                .build();
    }

    @Override
    @Transactional
    public KanbanWeeklyPlanResponseDTO getWeeklyPlan(String username, LocalDate weekStart) {
        Person user = getUser(username);
        LocalDate start = weekStart == null ? LocalDate.now() : weekStart;
        LocalDate end = start.plusDays(6);
        syncBoardItems(user, start, end);

        List<KanbanTask> cards = kanbanTaskRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscPositionAsc(
                user.getId(), start, end
        );
        List<KanbanTask> movable = cards.stream().filter(card -> !Boolean.TRUE.equals(card.getIsFixed())).toList();
        int capacity = Math.max(1, estimateHistoricalDailyCapacity(user.getId()));
        Map<LocalDate, List<KanbanTask>> byDay = movable.stream().collect(Collectors.groupingBy(KanbanTask::getScheduledDate));

        List<KanbanWeeklyPlanDayDTO> days = new ArrayList<>();
        Set<String> globalActions = new HashSet<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            List<KanbanTask> dayCards = byDay.getOrDefault(d, List.of());
            int planned = dayCards.size();
            String loadLevel = planned > capacity ? "OVERLOADED" : (planned == 0 ? "FREE" : "NORMAL");
            List<String> actions = new ArrayList<>();
            if ("OVERLOADED".equals(loadLevel)) {
                actions.add("Перенести 1-2 задачи на соседние дни");
                globalActions.add("Снизить нагрузку в перегруженные дни");
            } else if ("FREE".equals(loadLevel)) {
                actions.add("Использовать день как буфер под рискованные дедлайны");
            } else {
                actions.add("Начинать с задач высокого приоритета");
            }

            LocalDate finalD = d;
            long risk = dayCards.stream().filter(card -> {
                LocalDate deadline = resolveLimitDate(card);
                return deadline != null && !finalD.isBefore(deadline.minusDays(1));
            }).count();
            if (risk > 0) {
                actions.add("Есть задачи у дедлайна: закрыть их в первой половине дня");
                globalActions.add("Увеличить буфер перед дедлайнами до 2 дней");
            }

            days.add(KanbanWeeklyPlanDayDTO.builder()
                    .date(d)
                    .plannedTasks(planned)
                    .capacity(capacity)
                    .loadLevel(loadLevel)
                    .actions(actions)
                    .build());
        }

        if (globalActions.isEmpty()) {
            globalActions.add("Текущий недельный план сбалансирован");
        }

        return KanbanWeeklyPlanResponseDTO.builder()
                .weekStart(start)
                .weekEnd(end)
                .totalPlannedTasks(movable.size())
                .capacityPerDay(capacity)
                .days(days)
                .globalActions(globalActions.stream().toList())
                .build();
    }

    private void syncBoardItems(Person user, LocalDate start, LocalDate end) {
        if ("ROLE_STUDENT".equalsIgnoreCase(user.getRole())) {
            syncStudentItems(user, start, end);
        } else if ("ROLE_TEACHER".equalsIgnoreCase(user.getRole())) {
            syncTeacherItems(user, start, end);
        }
    }

    private void syncStudentItems(Person user, LocalDate start, LocalDate end) {
        List<TaskAssignment> assignments = taskAssignmentRepository.findByUserId(user.getId());
        Set<Long> activeTaskIds = assignments.stream()
                .filter(a -> a.getTask() != null && a.getTask().getDeadline() != null)
                .filter(a -> !"COMPLETED".equalsIgnoreCase(a.getStatus()))
                .filter(a -> !"OVERDUE".equalsIgnoreCase(a.getStatus()))
                .map(a -> {
                    ensureTaskCard(user, a.getTask(), start, end);
                    return a.getTaskId();
                })
                .collect(Collectors.toSet());

        cleanupStudentTaskCards(user.getId(), activeTaskIds);

        Long groupId = groupService.getUserGroup(user.getUsername());
        List<VideoMeeting> meetings = groupId == null
                ? videoMeetingRepository.findActiveWithoutGroup()
                : videoMeetingRepository.findActiveForStudent(groupId);
        meetings.stream()
                .filter(m -> m.getStartTime() != null)
                .forEach(m -> ensureMeetingCard(user, m));
    }

    private void syncTeacherItems(Person user, LocalDate start, LocalDate end) {
        List<Task> tasks = taskRepository.findByAuthorId(user.getId());
        tasks.stream()
                .filter(t -> t.getDeadline() != null)
                .forEach(t -> ensureTaskCard(user, t, start, end));

        List<VideoMeeting> meetings = videoMeetingRepository.findActiveByCreatedById(user.getId());
        meetings.stream()
                .filter(m -> m.getStartTime() != null)
                .forEach(m -> ensureMeetingCard(user, m));
    }

    private int estimateHistoricalDailyCapacity(Long userId) {
        List<TaskAssignment> assignments = taskAssignmentRepository.findByUserId(userId);
        LocalDateTime from = LocalDateTime.now().minusDays(HISTORY_DAYS);
        long completed = assignments.stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()))
                .filter(a -> a.getUpdated_At() != null && a.getUpdated_At().isAfter(from))
                .count();
        int weekly = (int) Math.round((completed / 4.0));
        if (weekly <= 0) {
            return 3;
        }
        return Math.max(2, Math.min(6, Math.max(1, (int) Math.ceil(weekly / 5.0) + 1)));
    }

    private KanbanCoachingInsightDTO buildCoachingInsight(Long userId, int effectiveDailyLimit) {
        List<TaskAssignment> assignments = taskAssignmentRepository.findByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last7 = now.minusDays(7);
        LocalDateTime last28 = now.minusDays(28);

        int completed7 = (int) assignments.stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()))
                .filter(a -> a.getUpdated_At() != null && a.getUpdated_At().isAfter(last7))
                .count();
        int completed28 = (int) assignments.stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()))
                .filter(a -> a.getUpdated_At() != null && a.getUpdated_At().isAfter(last28))
                .count();
        int overdue28 = (int) assignments.stream()
                .filter(a -> "OVERDUE".equalsIgnoreCase(a.getStatus()))
                .filter(a -> a.getUpdated_At() != null && a.getUpdated_At().isAfter(last28))
                .count();

        double avgWeek = completed28 / 4.0;
        int suggestedWeeklyLoad = Math.max(2, (int) Math.floor(avgWeek) - (overdue28 > 2 ? 1 : 0));
        String burnoutRisk = overdue28 >= 4 ? "HIGH" : (overdue28 >= 2 ? "MEDIUM" : "LOW");
        String advice = switch (burnoutRisk) {
            case "HIGH" -> "За последний месяц много просрочек. Снизьте недельную нагрузку и добавьте больше буфера перед дедлайнами.";
            case "MEDIUM" -> "Нагрузка на грани. Планируйте сначала приоритетные задачи и оставляйте минимум один свободный день в неделю.";
            default -> "Темп стабильный. Можно сохранять текущую нагрузку и фокусироваться на задачах с ближайшими дедлайнами.";
        };

        if (effectiveDailyLimit > 4 && "LOW".equals(burnoutRisk)) {
            advice = "Хорошая динамика закрытия задач. Можно держать " + effectiveDailyLimit + " задач в день, но сохраняйте резерв перед дедлайнами.";
        }

        return KanbanCoachingInsightDTO.builder()
                .completedLast7Days(completed7)
                .completedLast28Days(completed28)
                .avgCompletedPerWeek(avgWeek)
                .overdueLast28Days(overdue28)
                .suggestedWeeklyLoad(suggestedWeeklyLoad)
                .burnoutRisk(burnoutRisk)
                .advice(advice)
                .build();
    }

    private Map<LocalDate, Integer> buildFixedLoadMap(List<KanbanTask> cards, LocalDate start, LocalDate end) {
        Map<LocalDate, Integer> load = new HashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            load.put(d, 0);
        }
        cards.stream()
                .filter(card -> Boolean.TRUE.equals(card.getIsFixed()))
                .forEach(card -> load.merge(card.getScheduledDate(), 1, Integer::sum));
        return load;
    }

    private PlanningCandidate toCandidate(KanbanTask card, LocalDate start, Map<Long, TaskAssignment> assignmentByTaskId) {
        LocalDate deadline = resolveLimitDate(card);
        String resolvedPriority = resolvePriority(card, assignmentByTaskId);
        int priority = priorityWeight(resolvedPriority);
        int score = kanbanPrioritizationService.calculateScore(
                start,
                deadline,
                resolvedPriority,
                Boolean.TRUE.equals(card.getIsOptimized())
        );
        return new PlanningCandidate(card, deadline, priority, score);
    }

    private String resolvePriority(KanbanTask card, Map<Long, TaskAssignment> assignmentByTaskId) {
        if (card.getTask() == null || card.getTask().getId() == null) {
            return null;
        }
        TaskAssignment assignment = assignmentByTaskId.get(card.getTask().getId());
        if (assignment != null && assignment.getPriority() != null) {
            return assignment.getPriority();
        }
        return card.getTask().getPriority();
    }

    private void cleanupStudentTaskCards(Long userId, Set<Long> activeTaskIds) {
        List<KanbanTask> existing = kanbanTaskRepository.findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscPositionAsc(
                userId,
                LocalDate.now().minusDays(30),
                LocalDate.now().plusDays(DEFAULT_DAYS + 30)
        );
        existing.stream()
                .filter(card -> card.getTask() != null)
                .filter(card -> !activeTaskIds.contains(card.getTask().getId()))
                .forEach(card -> kanbanTaskRepository.deleteById(card.getId()));
    }

    private void ensureTaskCard(Person user, Task task, LocalDate start, LocalDate end) {
        kanbanTaskRepository.findByUserIdAndTaskId(user.getId(), task.getId()).orElseGet(() -> {
        LocalDate deadlineDate = task.getDeadline().toLocalDate();
        LocalDate scheduled;
        
        if ("ROLE_TEACHER".equalsIgnoreCase(user.getRole())) {
            // Преподаватель видит задачи на дату дедлайна
            scheduled = deadlineDate;
        } else {
            // Студент видит задачи спланированными на сегодня-дедлайн
            scheduled = LocalDate.now();
            if (scheduled.isBefore(start)) scheduled = start;
            if (scheduled.isAfter(end)) scheduled = end;
            if (deadlineDate.isBefore(scheduled)) scheduled = deadlineDate;
        }
        
        return saveNewCard(user, task, null, scheduled, false);
    });
    }

    private void ensureMeetingCard(Person user, VideoMeeting meeting) {
        kanbanTaskRepository.findByUserIdAndMeetingId(user.getId(), meeting.getId()).orElseGet(() -> {
            LocalDate scheduled = meeting.getStartTime().toLocalDate();
            return saveNewCard(user, null, meeting, scheduled, true);
        });
    }

    private KanbanTask saveNewCard(Person user, Task task, VideoMeeting meeting, LocalDate scheduledDate, boolean fixed) {
        KanbanTask item = new KanbanTask();
        item.setUser(user);
        item.setTask(task);
        item.setMeeting(meeting);
        item.setScheduledDate(scheduledDate);
        item.setIsFixed(fixed);
        item.setIsOptimized(false);
        int position = kanbanTaskRepository.findByUserIdAndScheduledDateOrderByPositionAsc(user.getId(), scheduledDate).size();
        item.setPosition(position);
        return kanbanTaskRepository.save(item);
    }

    private LocalDate resolveLimitDate(KanbanTask card) {
        if (card.getTask() != null && card.getTask().getDeadline() != null) {
            return card.getTask().getDeadline().toLocalDate();
        }
        if (card.getMeeting() != null && card.getMeeting().getStartTime() != null) {
            return card.getMeeting().getStartTime().toLocalDate();
        }
        return null;
    }

    private int priorityWeight(String priority) {
        if (priority == null) {
            return 10;
        }
        return switch (priority.toUpperCase(Locale.ROOT)) {
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 4;
        };
    }

    private String buildReason(LocalDate deadline, String priority, int buffer) {
        if (deadline == null) {
            return "Перенос для балансировки нагрузки по дням";
        }
        return "Дедлайн " + deadline + ", приоритет " + (priority == null ? "NORMAL" : priority) +
                ", буфер " + buffer + " дн.";
    }

    private void reindex(List<KanbanTask> rows) {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setPosition(i);
        }
    }

    private KanbanItemDTO toItemDto(KanbanTask entity) {
        Task task = entity.getTask();
        VideoMeeting meeting = entity.getMeeting();
        return KanbanItemDTO.builder()
                .id(entity.getId())
                .itemType(task != null ? "TASK" : "MEETING")
                .itemId(task != null ? task.getId() : (meeting != null ? meeting.getId() : null))
                .title(task != null ? task.getTitle() : (meeting != null ? meeting.getTitle() : null))
                .scheduledDate(entity.getScheduledDate())
                .position(entity.getPosition())
                .isFixed(entity.getIsFixed())
                .isOptimized(entity.getIsOptimized())
                .deadline(task != null ? task.getDeadline() : null)
                .priority(task != null ? task.getPriority() : null)
                .startTime(meeting != null ? meeting.getStartTime() : null)
                .endTime(meeting != null ? meeting.getEndTime() : null)
                .build();
    }

    private Person getUser(String username) {
        return peopleRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found", username));
    }

    private int scenarioScore(KanbanOptimizationResponseDTO response) {
        int risk = response.getRiskTasksCount() == null ? 0 : response.getRiskTasksCount();
        int overload = response.getOverloadedDays() == null ? 0 : response.getOverloadedDays();
        int suggestions = response.getSuggestions() == null ? 0 : response.getSuggestions().size();
        return risk * 10 + overload * 6 + suggestions;
    }

    private record PlanningCandidate(KanbanTask card, LocalDate deadline, int priorityWeight, int score) {}
    private record ScenarioCandidate(int dailyLimit, int bufferDays, int score, KanbanOptimizationResponseDTO result) {}
}
