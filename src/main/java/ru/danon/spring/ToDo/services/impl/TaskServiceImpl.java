package ru.danon.spring.ToDo.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.danon.spring.ToDo.dto.MyTaskDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.dto.SolutionDTO;
import ru.danon.spring.ToDo.dto.StatusDTO;
import ru.danon.spring.ToDo.dto.TagDTO;
import ru.danon.spring.ToDo.dto.TaskPriorityDTO;
import ru.danon.spring.ToDo.dto.TaskDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.dto.TaskStatDTO;
import ru.danon.spring.ToDo.enums.TaskStatus;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.mappers.TaskMapper;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.models.postgre.Tag;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.models.postgre.TaskAssignment;
import ru.danon.spring.ToDo.models.postgre.TaskFile;
import ru.danon.spring.ToDo.models.postgre.id.TaskAssignmentId;
import ru.danon.spring.ToDo.repositories.jpa.TaskAssignmentRepository;
import ru.danon.spring.ToDo.repositories.jpa.KanbanTaskRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskTagRepository;
import ru.danon.spring.ToDo.services.FileStorageService;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.NotificationProducerService;
import ru.danon.spring.ToDo.services.NotificationSchedulingService;
import ru.danon.spring.ToDo.services.PeopleService;
import ru.danon.spring.ToDo.services.TagService;
import ru.danon.spring.ToDo.services.TaskFileService;
import ru.danon.spring.ToDo.services.TaskService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
@Slf4j
public class TaskServiceImpl implements TaskService {
    private static final Set<String> ALLOWED_PRIORITY = Set.of("HIGH", "MEDIUM", "LOW");

    private final PeopleService peopleService;
    private final GroupService groupServiceImpl;
    private final TaskRepository taskRepository;
    private final TaskTagRepository taskTagRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final KanbanTaskRepository kanbanTaskRepository;
    private final NotificationProducerService notificationProducerServiceImpl;
    private final TaskMapper taskMapper;
    private final TagService tagServiceImpl;
    private final TaskFileService taskFileService;
    private final FileStorageService fileStorageServiceImpl;
    private final NotificationSchedulingService notificationSchedulingServiceImpl;

    @Transactional
    @Override
    public TaskResponseDTO createTask(MyTaskDTO taskDTO, String username) {
        log.info("Создание задачи пользователем: {}", username);

        Person author = peopleService.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Автор {} не найден при создании задачи", username);
                    return new EntityNotFoundException("Author not found", username);
                });

        Task task = new Task();
        task.setTitle(taskDTO.getTitle());
        task.setDescription(taskDTO.getDescription());
        task.setDeadline(taskDTO.getDeadline());
        task.setPriority("MEDIUM");
        task.setAuthor(author);
        task.setCreatedAt(LocalDateTime.now());

        Task savedTask = taskRepository.save(task);
        taskRepository.flush();
        log.info("Задача создана: id={}, title={}", savedTask.getId(), savedTask.getTitle());

        // Добавляем теги
        if (taskDTO.getTagIds() != null && !taskDTO.getTagIds().isEmpty()) {
            log.debug("Добавление тегов по ID к задаче id={}: {}", savedTask.getId(), taskDTO.getTagIds());
            for (Long tagId : taskDTO.getTagIds()) {
                try {
                    tagServiceImpl.addTagToTask(savedTask.getId(), tagId);
                    log.debug("Добавлен тег id={} к задаче id={}", tagId, savedTask.getId());
                } catch (Exception e) {
                    log.error("Ошибка при добавлении тега id={} к задаче id={}: {}", tagId, savedTask.getId(), e.getMessage());
                }
            }
        }

        // Добавляем новые теги по имени
        if (taskDTO.getTagNames() != null && !taskDTO.getTagNames().isEmpty()) {
            log.debug("Добавление тегов по имени к задаче id={}: {}", savedTask.getId(), taskDTO.getTagNames());
            for (String tagName : taskDTO.getTagNames()) {
                try {
                    tagServiceImpl.addTagToTaskByName(savedTask.getId(), tagName.trim());
                    log.debug("Добавлен тег '{}' к задаче id={}", tagName, savedTask.getId());
                } catch (Exception e) {
                    log.error("Ошибка при добавлении тега '{}' к задаче id={}: {}", tagName, savedTask.getId(), e.getMessage());
                }
            }
        }
        return taskMapper.convertToResponseDto(savedTask);
    }

    //удалить таску
    @Transactional
    @Override
    public void deleteTask(Long taskId) {
        log.info("Удаление задачи id={}", taskId);

        notificationSchedulingServiceImpl.cancelAllTaskNotifications(taskId);
        taskAssignmentRepository.deleteByTaskId(taskId);
        taskTagRepository.deleteByTaskId(taskId);
        taskRepository.deleteById(taskId);

        log.info("Задача id={} успешно удалена", taskId);
    }

    //просмотреть все созданные таски
    @Override
    public List<Task> findAllTasks() {
        log.debug("Получение всех задач");
        List<Task> tasks = taskRepository.findAll();
        log.debug("Найдено {} задач", tasks.size());
        return tasks;
    }

    @Override
    public Page<Task> findAllTasks(Pageable pageable) {
        log.debug("Получение всех задач с пагинацией: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return taskRepository.findAll(pageable);
    }


    //посмотреть конкретную таску
    @Override
    public Task findTaskById(Long taskId) {
        log.debug("Поиск задачи id={}", taskId);
        return taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.error("Задача id={} не найдена", taskId);
                    return new EntityNotFoundException("Task not found", taskId);
                });
    }

    //назначить таску юзеру(функция для препода)
    @Transactional
    @Override
    @CacheEvict(cacheNames = {"studentActiveTasks", "kanbanBoard"}, allEntries = true)
    public void assignTask(Long taskId, Long userId, String currentUsername) {
        log.info("Назначение задачи id={} пользователю id={} преподавателем: {}", taskId, userId, currentUsername);

        if (taskAssignmentRepository.existsById(new TaskAssignmentId(taskId, userId))) {
            log.warn("Задача id={} уже назначена пользователю id={}", taskId, userId);
            throw new IllegalArgumentException("Task assignment already exists");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.error("Задача id={} не найдена при назначении", taskId);
                    return new EntityNotFoundException("Task not found", taskId);
                });
        Person user = peopleService.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь id={} не найден при назначении задачи", userId);
                    return new EntityNotFoundException("User not found", userId);
                });

        Person assignedBy = peopleService.findByUsername(currentUsername)
                .orElseThrow(() -> {
                    log.error("Преподаватель {} не найден", currentUsername);
                    return new EntityNotFoundException("Current user not found", currentUsername);
                });

        if (taskAssignmentRepository.existsByTaskAndUser(task, user)) {
            log.warn("Задача id={} уже назначена пользователю id={}", taskId, userId);
            throw new IllegalArgumentException("Task assignment already exists");
        }

        TaskAssignment taskAssignment = new TaskAssignment();
        taskAssignment.setTaskId(taskId);
        taskAssignment.setUserId(userId);
        taskAssignment.setTask(task);
        taskAssignment.setUser(user);
        taskAssignment.setAssignedBy(assignedBy);
        taskAssignment.setAssignedAt(LocalDateTime.now());
        taskAssignment.setUpdated_At(LocalDateTime.now());
        taskAssignment.setPriority(resolveAssignmentPriority(task, null));

        notificationSchedulingServiceImpl.scheduleTaskNotifications(taskAssignment);
        taskAssignmentRepository.save(taskAssignment);

        log.info("Задача id={} успешно назначена пользователю id={}", taskId, userId);

        //уведомление: вам назначена новая задача
        notificationProducerServiceImpl.sendTaskAssignedNotification(
                userId,
                user.getRole(),
                task.getTitle(),
                taskId
        );
    }

    //назначить таску группе по её Id (функция для препода)
    @Transactional
    @Override
    @CacheEvict(cacheNames = {"studentActiveTasks", "kanbanBoard"}, allEntries = true)
    public void assignTaskForGroup(Long taskID, Long groupId, String currentUsername) {
        log.info("Назначение задачи id={} группе id={} преподавателем: {}", taskID, groupId, currentUsername);

        List<Person> groupMembers = groupServiceImpl.getPersonsByGroupId(groupId);

        // Находим пользователей, которым задача еще не назначена
        List<Person> usersToAssign = groupMembers.stream()
                .filter(user -> !taskAssignmentRepository.existsById(
                        new TaskAssignmentId(taskID, user.getId())))
                .toList();

        // Назначаем задачу только тем, у кого ее еще нет
        for (Person user : usersToAssign) {
            assignTask(taskID, user.getId(), currentUsername);
        }

        log.info("Задача id={} назначена {} пользователям группы id={}, пропущено {} (уже назначено)",
                taskID, usersToAssign.size(), groupId, groupMembers.size() - usersToAssign.size());
    }

    //получить статус таски (функция для препода)
    @Override
    public TaskStatDTO findStatusTask(Long id, Long taskId, String filter) {
        log.debug("Получение статуса задачи id={} для {} id={}", taskId, filter, id);

        TaskStatDTO taskDTO = new TaskStatDTO();
        taskDTO.setId(taskId);

        if ("student".equalsIgnoreCase(filter)) {
            // Получаем назначение → статус у него
            TaskAssignment assignment = taskAssignmentRepository.findByUserIdAndTaskId(id, taskId)
                    .orElseThrow(() -> {
                        log.error("Задача id={} не назначена пользователю id={}", taskId, id);
                        return new RuntimeException("Задача не назначена пользователю");
                    });

            taskDTO.setStatus(assignment.getStatus());
            taskDTO.setUserId(id);

        } else if ("group".equalsIgnoreCase(filter)) {
            // Получаем все назначения задачи в группе
            List<TaskAssignment> assignments = taskAssignmentRepository.findByGroupIdAndTaskId(id, taskId);

            // Инициализируем счётчики по всем возможным статусам
            Map<String, Long> statusStatistics = new HashMap<>();
            for (TaskStatus status : TaskStatus.values()) {
                statusStatistics.put(status.name(), 0L);
            }

            // Считаем статусы из назначений
            assignments.forEach(assignment -> {
                String status = assignment.getStatus();
                if (statusStatistics.containsKey(status)) {
                    statusStatistics.put(status, statusStatistics.get(status) + 1);
                } else {
                    // На случай, если статус не в enum (например, OVERDUE)
                    statusStatistics.merge("OTHER", 1L, Long::sum);
                }
            });

            taskDTO.setStatusStatistics(statusStatistics);
            taskDTO.setGroupId(id);

        } else {
            log.error("Неверный фильтр: {}", filter);
            throw new IllegalArgumentException("Неверный фильтр. Используйте 'student' или 'group'");
        }

        return taskDTO;
    }


    @Override
    public Page<MyTaskDTO> findMyTasks(String username, Pageable pageable) {
        log.debug("Получение задач пользователя: {}, page={}, size={}", username, pageable.getPageNumber(), pageable.getPageSize());

        Person user = peopleService.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Пользователь {} не найден", username);
                    return new EntityNotFoundException("Пользователь не найден: ", username);
                });

        Page<TaskAssignment> assignments = taskAssignmentRepository.findByUser(user, pageable);
        if (assignments.isEmpty())
            return Page.empty(pageable);
        Page<MyTaskDTO> result = mapAssignmentsToMyTaskPage(assignments);
        log.debug("Найдено {} задач для пользователя {}", result.getNumberOfElements(), username);
        return result;
    }

    @Override
    @Cacheable(cacheNames = "studentActiveTasks", key = "#username + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<MyTaskDTO> findMyActiveTasks(String username, Pageable pageable) {
        Person user = peopleService.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: ", username));
        Page<TaskAssignment> assignments = taskAssignmentRepository.findActiveByUser(user, pageable);
        if (assignments.isEmpty()) {
            return Page.empty(pageable);
        }
        return mapAssignmentsToMyTaskPage(assignments);
    }


    @Override
    public Page<MyTaskDTO> findUserTasks(Long userId, Pageable pageable) {
        log.debug("Получение задач пользователя id={}, page={}, size={}", userId, pageable.getPageNumber(), pageable.getPageSize());

        Person user = peopleService.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь id={} не найден", userId);
                    return new EntityNotFoundException("Пользователь не найден", userId);
                });

        Page<TaskAssignment> assignments = taskAssignmentRepository.findByUser(user, pageable);
        if (assignments.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<MyTaskDTO> result = mapAssignmentsToMyTaskPage(assignments);
        log.debug("Найдено {} задач для пользователя id={}", result.getNumberOfElements(), userId);
        return result;
    }

    //юзер ищет свою конкретную таску
    @Override
    public MyTaskDTO findMyTasksById(Long taskId, String currentUsername) {
        log.debug("Получение задачи id={} пользователем: {}", taskId, currentUsername);

        Person currentUser = peopleService.findByUsername(currentUsername)
                .orElseThrow(() -> {
                    log.error("Пользователь {} не найден", currentUsername);
                    return new EntityNotFoundException("User not found", currentUsername);
                });

        // Создаём составной ID
        TaskAssignmentId assignmentId = new TaskAssignmentId(taskId, currentUser.getId());

        // Находим назначение задачи (включает статус!)
        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> {
                    log.error("Задача id={} не назначена пользователю {}", taskId, currentUsername);
                    return new EntityNotFoundException("Task not found or not assigned to you");
                });

        // Достаём саму задачу и её статус
        Task task = assignment.getTask();
        String status = assignment.getStatus();
        Long authorId = task.getAuthor() != null ? task.getAuthor().getId() : null;

        List<Tag> taskTags = tagServiceImpl.getTaskTags(taskId);
        List<TagDTO> tags = new ArrayList<>();
        for (Tag tag : taskTags) {
            tags.add(convertToTagDTO(tag));
        }

        return new MyTaskDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDeadline(),
                normalizePriority(resolveAssignmentPriority(task, assignment.getPriority())),
                authorId,
                status,
                tags
        );
    }


    //юзер получает статус конкретной таски
    @Override
    public StatusDTO findStatusMyTask(Long taskId, String currentUsername) {
        log.debug("Получение статуса задачи id={} пользователем: {}", taskId, currentUsername);

        Long myId = peopleService.findByUsername(currentUsername).get().getId();
        TaskAssignmentId id = new TaskAssignmentId(taskId, myId);
        TaskAssignment assignment = taskAssignmentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Задача id={} не назначена пользователю {}", taskId, currentUsername);
                    return new EntityNotFoundException("Task not found or not assigned to you");
                });

        String status = assignment.getStatus();
        return new StatusDTO(status);
    }

    //юзер делится таской с другим юзером
    @Transactional
    @Override
    public void shareTask(Long taskId, Long userId, String currentUsername) {
        log.info("Передача задачи id={} пользователю id={} от: {}", taskId, userId, currentUsername);

        Person currentPerson = peopleService.findByUsername(currentUsername)
                .orElseThrow(() -> {
                    log.error("Пользователь {} не найден", currentUsername);
                    return new EntityNotFoundException("Current user not found");
                });

        TaskAssignmentId senderId = new TaskAssignmentId(taskId, currentPerson.getId());
        if (!taskAssignmentRepository.existsById(senderId)) {
            log.warn("Пользователь {} пытается поделиться задачей id={}, которой у него нет", currentUsername, taskId);
            throw new AccessDeniedException("You don't have this task to share");
        }

        TaskAssignmentId receiverId = new TaskAssignmentId(taskId, userId);
        if (taskAssignmentRepository.existsById(receiverId)) {
            log.warn("Пользователь id={} уже имеет задачу id={}", userId, taskId);
            throw new IllegalArgumentException("User already has this task");
        }

        assignTask(taskId, userId, currentUsername);
        log.info("Задача id={} успешно передана от {} пользователю id={}", taskId, currentUsername, userId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"studentActiveTasks", "kanbanBoard"}, allEntries = true)
    public int deleteMyOverdueAssignments(String username) {
        Person user = peopleService.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: ", username));
        List<TaskAssignment> overdue = taskAssignmentRepository.findByUserAndStatus(
                user,
                "OVERDUE",
                Sort.by(Sort.Direction.ASC, "taskId")
        );
        overdue.forEach(a -> kanbanTaskRepository.deleteByUserIdAndTaskId(user.getId(), a.getTaskId()));
        return taskAssignmentRepository.deleteOverdueAssignmentsByUserId(user.getId());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"studentActiveTasks", "kanbanBoard"}, allEntries = true)
    public MyTaskDTO updateMyTaskPriority(Long taskId, TaskPriorityDTO taskPriorityDTO, String username) {
        String normalizedPriority = normalizePriority(taskPriorityDTO.getPriority());
        Person user = peopleService.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found", username));
        TaskAssignment assignment = taskAssignmentRepository
                .findById(new TaskAssignmentId(taskId, user.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Task not found or not assigned to you"));
        assignment.setPriority(normalizedPriority);
        assignment.setUpdated_At(LocalDateTime.now());
        taskAssignmentRepository.save(assignment);
        return findMyTasksById(taskId, username);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"studentActiveTasks", "kanbanBoard"}, allEntries = true)
    public Map<String, Integer> assignTasksForGroups(List<Long> taskIds, List<Long> groupIds, String currentUsername) {
        if (taskIds == null || taskIds.isEmpty() || groupIds == null || groupIds.isEmpty()) {
            throw new IllegalArgumentException("Нужно передать taskIds и groupIds");
        }
        int created = 0;
        int skipped = 0;
        for (Long taskId : taskIds) {
            for (Long groupId : groupIds) {
                List<Person> groupMembers = groupServiceImpl.getPersonsByGroupId(groupId);
                for (Person member : groupMembers) {
                    if (taskAssignmentRepository.existsById(new TaskAssignmentId(taskId, member.getId()))) {
                        skipped++;
                        continue;
                    }
                    assignTask(taskId, member.getId(), currentUsername);
                    created++;
                }
            }
        }
        return Map.of("created", created, "skipped", skipped);
    }

    @Override
    public Page<Task> findAllActiveTasks(Pageable pageable) {
        log.debug("Получение активных задач");
        return taskRepository.findActiveTasks(pageable);
    }

    @Override
    public Set<TaskResponseDTO> getGroupTasks(Long groupId) {
        log.debug("Получение задач группы id={}", groupId);

        List<Person> groupMembers = groupServiceImpl.getPersonsByGroupId(groupId);

        if (groupMembers.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> memberIds = groupMembers.stream()
                .map(Person::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Set<TaskResponseDTO> tasks = taskAssignmentRepository.findByUserIdIn(memberIds).stream()
                .map(TaskAssignment::getTask)
                .filter(Objects::nonNull)
                .map(taskMapper::toResponseDto)
                .collect(Collectors.toCollection(HashSet::new));

        log.debug("Найдено {} задач для группы id={}", tasks.size(), groupId);
        return tasks;
    }

    @Transactional
    @Override
    @CacheEvict(cacheNames = {"studentActiveTasks", "kanbanBoard"}, allEntries = true)
    public void updateOverdueTasks() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Проверка просроченных задач, время: {}", now);

        // Получаем все задания, которые станут просроченными
        List<TaskAssignment> overdueAssignments = taskAssignmentRepository.findOverdueTaskAssignments(now);

        if (!overdueAssignments.isEmpty()) {
            log.info("Найдено {} просроченных задач", overdueAssignments.size());

            // Обновляем статус
            taskAssignmentRepository.updateOverdueTaskAssignments(now);

            // Отправляем уведомления
            for (TaskAssignment assignment : overdueAssignments) {
                notificationProducerServiceImpl.sendTaskOverdueNotification(
                        assignment.getUserId(),
                        "ROLE_STUDENT",
                        assignment.getTask().getTitle(),
                        assignment.getTask().getId()
                );

                notificationSchedulingServiceImpl.cancelTaskNotifications(
                        assignment.getTask().getId(),
                        assignment.getUserId()
                );
            }
        }
    }

    @Override
    public List<PersonResponseDTO> getUsersWithTask(Long taskId, Authentication auth) {
        log.debug("Получение пользователей с задачей id={} для {}", taskId, auth.getName());

        Person user = peopleService.findByUsername(auth.getName())
                .orElseThrow(() -> {
                    log.error("Пользователь {} не найден", auth.getName());
                    return new EntityNotFoundException("User not found");
                });

        List<PersonResponseDTO> users;
        if (groupServiceImpl.isTeacher(user)) {
            users = groupServiceImpl.findByTeacherId(auth)
                    .stream()
                    .filter(person -> taskAssignmentRepository.existsById(
                            new TaskAssignmentId(taskId, person.getId())
                    )).map(
                            taskMapper::convertToPersonDTO
                    )
                    .toList();
        } else {
            users = groupServiceImpl.getPersonsByGroupId(groupServiceImpl.getUserGroup(auth.getName()))
                    .stream()
                    .filter(person -> !person.getId().equals(user.getId())) // убираем текущего пользователя
                    .filter(person -> {
                        try {
                            findMyTasksById(taskId, person.getUsername());
                            return true;
                        } catch (EntityNotFoundException e) {
                            return false;
                        }
                    }).map(
                            taskMapper::convertToPersonDTO
                    )
                    .toList();
        }

        log.debug("Найдено {} пользователей с задачей id={}", users.size(), taskId);
        return users;
    }


    private TagDTO convertToTagDTO(Tag tag){
        if (tag == null) return null;
        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        return dto;
    }

    private List<TagDTO> toTagDTOs(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }

        return tags.stream()
                .map(this::convertToTagDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }



    @Transactional
    @Override
    public Task updateTask(Long taskId, TaskDTO task, String username) {
        log.info("Обновление задачи id={} пользователем: {}", taskId, username);

        Person person = peopleService.findByUsername(username).orElseThrow(
                () -> {
                    log.error("Пользователь {} не найден при обновлении задачи", username);
                    return new EntityNotFoundException("User not found", username);
                });

        Task oldTask = taskRepository.findById(taskId).orElseThrow(
                () -> {
                    log.error("Задача id={} не найдена при обновлении", taskId);
                    return new EntityNotFoundException("Task not found", taskId);
                });

        if (oldTask.getAuthor() == null) {
            log.error("Задача id={} не имеет автора", taskId);
            throw new RuntimeException("Task has no author - cannot determine permissions");
        }

        if(!person.getId().equals(oldTask.getAuthor().getId())){
            log.warn("Пользователь {} пытается редактировать чужую задачу id={}", username, taskId);
            throw new AccessDeniedException("You can only edit your own tasks");
        }
        LocalDateTime oldDeadline = oldTask.getDeadline();

        oldTask.setTitle(task.getTitle());
        oldTask.setDescription(task.getDescription());
        oldTask.setDeadline(task.getDeadline());
        oldTask.setPriority("MEDIUM");

        Task updatedTask = taskRepository.save(oldTask);
        updateTaskTags(updatedTask, task);

        log.info("Задача id={} успешно обновлена", taskId);

        if (!oldDeadline.equals(task.getDeadline())) {
            log.debug("Дедлайн задачи id={} изменен, обновление статусов назначений", taskId);
            updateTaskAssignmentsStatus(updatedTask);

            // Перепланируем уведомления для всех назначений
            List<TaskAssignment> assignments = taskAssignmentRepository.findByTask(updatedTask);
            for (TaskAssignment assignment : assignments) {
                // Отменяем только если задача не завершена и не просрочена
                if (!"COMPLETED".equals(assignment.getStatus()) && !"OVERDUE".equals(assignment.getStatus())) {
                    notificationSchedulingServiceImpl.rescheduleTaskNotifications(assignment);
                }
            }
        }

        return updatedTask;
    }



    private void updateTaskTags(Task oldTask, TaskDTO newTask) {
        // Получаем текущие теги задачи - используем ID старой задачи
        List<Tag> currentTags = tagServiceImpl.getTaskTags(oldTask.getId());
        Set<Long> currentTagIds = currentTags.stream()
                .map(Tag::getId)
                .collect(Collectors.toSet());

        // Обрабатываем теги по ID
        if (newTask.getTagIds() != null && !newTask.getTagIds().isEmpty()) {
            log.debug("Обновление тегов по ID для задачи id={}", oldTask.getId());
            Set<Long> newTagIds = new HashSet<>(newTask.getTagIds());

            // Удаляем теги, которых нет в новом списке
            for (Tag currentTag : currentTags) {
                if (!newTagIds.contains(currentTag.getId())) {
                    try {
                        log.debug("Удаление тега id={} из задачи id={}", currentTag.getId(), oldTask.getId());
                        tagServiceImpl.removeTagFromTask(oldTask.getId(), currentTag.getId());
                    } catch (Exception e) {
                        log.error("Ошибка при удалении тега id={} из задачи id={}: {}", currentTag.getId(), oldTask.getId(), e.getMessage());
                    }
                }
            }

            // Добавляем новые теги
            for (Long tagId : newTask.getTagIds()) {
                if (!currentTagIds.contains(tagId)) {
                    try {
                        log.debug("Добавление тега id={} к задаче id={}", tagId, oldTask.getId());
                        tagServiceImpl.addTagToTask(oldTask.getId(), tagId);
                    } catch (Exception e) {
                        log.error("Ошибка при добавлении тега id={} к задаче id={}: {}", tagId, oldTask.getId(), e.getMessage());
                    }
                }
            }
        }

        // Добавляем новые теги по имени
        if (newTask.getTagNames() != null && !newTask.getTagNames().isEmpty()) {
            for (String tagName : newTask.getTagNames()) {
                try {
                    tagServiceImpl.addTagToTaskByName(oldTask.getId(), tagName.trim());
                } catch (Exception e) {
                    log.error("Ошибка при добавлении тега '{}' к задаче id={}: {}", tagName, oldTask.getId(), e.getMessage());
                }
            }
        }
    }


    /**
     * Загружает решение для задачи (студент)
     */
    @Transactional
    @Override
    @CacheEvict(cacheNames = {"studentActiveTasks", "kanbanBoard"}, allEntries = true)
    public void uploadSolution(Long taskId, MultipartFile file, String username) {
        log.info("Загрузка решения к задаче id={} студентом: {}", taskId, username);

        Person student = peopleService.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Студент {} не найден", username);
                    return new EntityNotFoundException("Студент не найден", username);
                });

        // Находим назначение
        TaskAssignment assignment = taskAssignmentRepository
                .findById(new TaskAssignmentId(taskId, student.getId()))
                .orElseThrow(() -> {
                    log.error("Задача id={} не назначена студенту {}", taskId, username);
                    return new EntityNotFoundException("Задача не назначена студенту", taskId);
                });

        // Проверяем дедлайн
        if (!assignment.canUploadSolution()) {
            log.warn("Студент {} пытается загрузить решение после дедлайна для задачи id={}", username, taskId);
            throw new RuntimeException("Нельзя загрузить решение после дедлайна");
        }

        // Если уже есть решение - удаляем старое
        if (assignment.hasSolution()) {
            log.debug("Удаление старого решения для задачи id={} студента {}", taskId, username);
            fileStorageServiceImpl.deleteFile(assignment.getSolutionFilePath());
        }

        // Генерируем путь для нового файла
        String storedFileName = fileStorageServiceImpl.generateFileName(file.getOriginalFilename());
        String filePath = String.format("tasks/%d/solutions/%d/%s",
                taskId, student.getId(), storedFileName);

        // Загружаем в MinIO
        fileStorageServiceImpl.uploadFile(file, filePath);

        // Обновляем назначение
        assignment.setSolutionFileName(file.getOriginalFilename());
        assignment.setSolutionFilePath(filePath);
        assignment.setSolutionFileSize(file.getSize());
        assignment.setSolutionUploadedAt(LocalDateTime.now());
        assignment.setStatus("COMPLETED");
        assignment.setUpdated_At(LocalDateTime.now());

        taskAssignmentRepository.save(assignment);

        log.info("Решение успешно загружено к задаче id={} студентом {}", taskId, username);

        notificationSchedulingServiceImpl.cancelTaskNotifications(taskId, student.getId());

        try {
            Person teacher = assignment.getTask().getAuthor();
            notificationProducerServiceImpl.sendSolutionUploadedNotification(
                    teacher.getId(),
                    teacher.getRole(),
                    student.getUsername(),
                    assignment.getTask().getTitle(),
                    taskId
            );
        } catch (Exception e) {
            log.error("Не удалось отправить уведомление о загрузке решения: {}", e.getMessage());
        }
    }

    /**
     * Ставит оценку за решение (преподаватель)
     */
    @Transactional
    @Override
    public void gradeSolution(Long taskId, Long studentId, Integer grade, String comment, String username) {
        log.info("Оценка решения: задача id={}, студент id={}, оценка={}, преподаватель: {}", taskId, studentId, grade, username);

        Person teacher = peopleService.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Преподаватель {} не найден", username);
                    return new EntityNotFoundException("Преподаватель не найден", username);
                });

        // Проверяем что преподаватель имеет право оценивать эту задачу
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.error("Задача id={} не найдена", taskId);
                    return new EntityNotFoundException("Задача не найдена", taskId);
                });

        if (!task.getAuthor().getId().equals(teacher.getId())) {
            log.warn("Преподаватель {} пытается оценить чужую задачу id={}", username, taskId);
            throw new AccessDeniedException("Можно оценивать только свои задачи");
        }

        if(grade > 100) {
            log.warn("Попытка поставить оценку > 100: {}", grade);
            throw new IllegalArgumentException("Grade must be less than 100");
        } else if(grade < 0) {
            log.warn("Попытка поставить оценку < 0: {}", grade);
            throw new IllegalArgumentException("Grade must be greater than 0");
        }

        TaskAssignment assignment = taskAssignmentRepository
                .findById(new TaskAssignmentId(taskId, studentId))
                .orElseThrow(() -> {
                    log.error("Назначение задачи id={} студенту id={} не найдено", taskId, studentId);
                    return new EntityNotFoundException("Назначение не найдено");
                });

        assignment.setGrade(grade);
        assignment.setTeacherComment(comment);

        taskAssignmentRepository.save(assignment);
        log.info("Оценка {} выставлена студенту id={} за задачу id={}", grade, studentId, taskId);

        try {
            Person student = assignment.getUser();

            notificationProducerServiceImpl.sendSolutionGradedNotification(
                    student.getId(),
                    student.getRole(),
                    teacher.getUsername(),
                    assignment.getTask().getTitle(),
                    grade,
                    comment,
                    taskId
            );
        } catch (Exception e) {
            log.error("Не удалось отправить уведомление об оценке решения: {}", e.getMessage());
        }
    }

    /**
     * Получает файлы условия задачи
     */
    @Override
    public List<TaskFile> getTaskFiles(Long taskId) {
        log.debug("Получение файлов задачи id={}", taskId);
        return taskFileService.getTaskFiles(taskId);
    }

    /**
     * Получает ссылку для скачивания решения
     */
    @Override
    public String getSolutionDownloadUrl(Long taskId, String username) {
        log.debug("Получение ссылки для скачивания решения: задача id={}, студент: {}", taskId, username);

        Person student = peopleService.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Студент {} не найден", username);
                    return new EntityNotFoundException("Студент не найден", username);
                });

        // Находим назначение
        TaskAssignment assignment = taskAssignmentRepository
                .findById(new TaskAssignmentId(taskId, student.getId()))
                .orElseThrow(() -> {
                    log.error("Задача id={} не назначена студенту {}", taskId, username);
                    return new RuntimeException("Задача не назначена студенту");
                });

        if (!assignment.hasSolution()) {
            log.warn("Решение не найдено для задачи id={} студента {}", taskId, username);
            throw new EntityNotFoundException("Решение не найдено");
        }

        // Генерируем ссылку для скачивания
        return fileStorageServiceImpl.generateDownloadUrl(assignment.getSolutionFilePath());
    }

    /**
     * Удаляет решение студента
     */
    @Transactional
    @Override
    @CacheEvict(cacheNames = {"studentActiveTasks", "kanbanBoard"}, allEntries = true)
    public void deleteSolution(Long taskId, String username) {
        log.info("Удаление решения к задаче id={} студентом: {}", taskId, username);

        Person student = peopleService.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Студент {} не найден", username);
                    return new EntityNotFoundException("Студент не найден", username);
                });

        // Находим назначение
        TaskAssignment assignment = taskAssignmentRepository
                .findById(new TaskAssignmentId(taskId, student.getId()))
                .orElseThrow(() -> {
                    log.error("Задача id={} не назначена студенту {}", taskId, username);
                    return new RuntimeException("Задача не назначена студенту");
                });

        if (!assignment.hasSolution()) {
            log.warn("Решение не найдено для задачи id={} студента {}", taskId, username);
            throw new EntityNotFoundException("Решение не найдено");
        }

        // Проверяем дедлайн - можно удалять только до дедлайна
        if (!assignment.canUploadSolution()) {
            log.warn("Студент {} пытается удалить решение после дедлайна для задачи id={}", username, taskId);
            throw new IllegalArgumentException("Нельзя удалить решение после дедлайна");
        }

        // Удаляем файл из MinIO
        fileStorageServiceImpl.deleteFile(assignment.getSolutionFilePath());

        // Очищаем информацию о решении
        assignment.setSolutionFileName(null);
        assignment.setSolutionFilePath(null);
        assignment.setSolutionFileSize(null);
        assignment.setSolutionUploadedAt(null);

        if(assignment.getGrade() != null || assignment.getTeacherComment() != null) {
            assignment.setGrade(null);
            assignment.setTeacherComment(null);
        }
        assignment.setStatus("IN_PROGRESS");
        assignment.setUpdated_At(LocalDateTime.now());
        taskAssignmentRepository.save(assignment);

        log.info("Решение успешно удалено: задача id={}, студент {}", taskId, username);
    }

    /**
     * Получает решение студента (для преподавателя)
     */
    @Override
    public SolutionDTO getStudentSolution(Long taskId, Authentication auth) {
        log.debug("Получение решения студента: задача id={}, пользователь: {}", taskId, auth.getName());

        Person user = peopleService.findByUsername(auth.getName()).orElseThrow(
                () -> {
                    log.error("Пользователь {} не найден", auth.getName());
                    return new EntityNotFoundException("user not found");
                });

        TaskAssignment assignment = taskAssignmentRepository
                .findById(new TaskAssignmentId(taskId, user.getId()))
                .orElseThrow(() -> {
                    log.error("Назначение не найдено: задача id={}, пользователь {}", taskId, auth.getName());
                    return new EntityNotFoundException("Назначение не найдено");
                });

       if (!assignment.hasSolution()) {
        SolutionDTO emptySolution = new SolutionDTO();
        emptySolution.setCanUpload(assignment.canUploadSolution());
        return emptySolution;
    }

        SolutionDTO solutionDTO = new SolutionDTO();
        solutionDTO.setFileName(assignment.getSolutionFileName());
        solutionDTO.setFileSize(assignment.getSolutionFileSize());
        solutionDTO.setUploadedAt(assignment.getSolutionUploadedAt());
        solutionDTO.setDownloadUrl(fileStorageServiceImpl.generateDownloadUrl(assignment.getSolutionFilePath()));
        solutionDTO.setGrade(assignment.getGrade());
        solutionDTO.setTeacherComment(assignment.getTeacherComment());
        solutionDTO.setCanUpload(assignment.canUploadSolution());

        return solutionDTO;
    }

    @Override
    public List<SolutionDTO> getAllSolutionsForTask(Long taskId, String teacherUsername) {
        log.debug("Получение всех решений для задачи id={} преподавателем: {}", taskId, teacherUsername);

        // Получаем все назначения для этой задачи
        List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(taskId);

        List<SolutionDTO> solutions = assignments.stream()
                .map(assignment -> {
                    SolutionDTO dto = new SolutionDTO();
                    dto.setStudentId(assignment.getUser().getId());
                    dto.setStudentName(assignment.getUser().getUsername());
                    dto.setFileName(assignment.getSolutionFileName());
                    dto.setFileSize(assignment.getSolutionFileSize());
                    dto.setUploadedAt(assignment.getSolutionUploadedAt());
                    dto.setGrade(assignment.getGrade());
                    dto.setTeacherComment(assignment.getTeacherComment());
                    dto.setCanUpload(assignment.getSolutionFileName() == null);
                    return dto;
                })
                .collect(Collectors.toList());

        log.debug("Найдено {} решений для задачи id={}", solutions.size(), taskId);
        return solutions;
    }

    @Override
    public String getStudentSolutionDownloadUrl(Long taskId, Long studentId, String teacherUsername) {
        log.debug("Получение ссылки на решение: задача id={}, студент id={}, преподаватель: {}", taskId, studentId, teacherUsername);

        TaskAssignment assignment = taskAssignmentRepository.findByUserIdAndTaskId(studentId, taskId)
                .orElseThrow(() -> {
                    log.error("Назначение не найдено: задача id={}, студент id={}", taskId, studentId);
                    return new EntityNotFoundException("Assignment not found");
                });

        // Проверка прав преподавателя
        if (!assignment.getTask().getAuthor().getUsername().equals(teacherUsername)) {
            log.warn("Преподаватель {} пытается скачать решение чужой задачи id={}", teacherUsername, taskId);
            throw new AccessDeniedException("Access denied");
        }

        if (assignment.getSolutionFilePath() == null) {
            log.warn("Решение не найдено: задача id={}, студент id={}", taskId, studentId);
            throw new EntityNotFoundException("Решение не найдено");
        }

        return fileStorageServiceImpl.generateDownloadUrl(assignment.getSolutionFilePath());
    }

    private Page<MyTaskDTO> mapAssignmentsToMyTaskPage(Page<TaskAssignment> assignments) {
        List<Long> taskIds = assignments.getContent().stream()
                .map(TaskAssignment::getTask)
                .filter(Objects::nonNull)
                .map(Task::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, List<Tag>> tagsByTask = tagServiceImpl.getTaskTagsBatch(taskIds);
        List<MyTaskDTO> content = assignments.getContent().stream()
                .map(assignment -> {
                    Task task = assignment.getTask();
                    Long authorId = task.getAuthor() != null ? task.getAuthor().getId() : null;
                    List<TagDTO> tags = toTagDTOs(tagsByTask.getOrDefault(task.getId(), Collections.emptyList()));
                    return new MyTaskDTO(
                            task.getId(),
                            task.getTitle(),
                            task.getDescription(),
                            task.getDeadline(),
                            normalizePriority(resolveAssignmentPriority(task, assignment.getPriority())),
                            authorId,
                            assignment.getStatus(),
                            tags
                    );
                }).toList();
        return new PageImpl<>(content, assignments.getPageable(), assignments.getTotalElements());
    }

    private String resolveAssignmentPriority(Task task, String assignmentPriority) {
        if (assignmentPriority != null && ALLOWED_PRIORITY.contains(assignmentPriority.toUpperCase(Locale.ROOT))) {
            return assignmentPriority.toUpperCase(Locale.ROOT);
        }
        if (task != null && task.getPriority() != null && ALLOWED_PRIORITY.contains(task.getPriority().toUpperCase(Locale.ROOT))) {
            return task.getPriority().toUpperCase(Locale.ROOT);
        }
        return "MEDIUM";
    }

    private String normalizePriority(String rawPriority) {
        String normalized = rawPriority == null ? "MEDIUM" : rawPriority.toUpperCase(Locale.ROOT);
        if (!ALLOWED_PRIORITY.contains(normalized)) {
            throw new IllegalArgumentException("Допустимые значения priority: HIGH, MEDIUM, LOW");
        }
        return normalized;
    }


    private TaskDTO convertToTaskDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setDeadline(task.getDeadline());
        dto.setPriority(task.getPriority());
        dto.setAuthorId(task.getAuthor() != null ? task.getAuthor().getId() : null);

        // Теги преобразуем вручную
        dto.setTags(toTagDTOs(tagServiceImpl.getTaskTags(task.getId())));

        return dto;
    }

    private void updateTaskAssignmentsStatus(Task task) {
        List<TaskAssignment> assignments = taskAssignmentRepository.findByTask(task);
        LocalDateTime now = LocalDateTime.now();

        for (TaskAssignment assignment : assignments) {
            String newStatus = calculateTaskStatus(task.getDeadline(), now, assignment.getStatus());
            if (!assignment.getStatus().equals(newStatus)) {
                assignment.setStatus(newStatus);
                taskAssignmentRepository.save(assignment);
                log.debug("Обновлен статус назначения для пользователя id={}: {}", assignment.getUserId(), assignment.getStatus());
            }
        }
    }

    private String calculateTaskStatus(LocalDateTime deadline, LocalDateTime now, String currentStatus) {
        // Если задача уже завершена - не меняем статус
        if ("COMPLETED".equals(currentStatus)) {
            return currentStatus;
        }

        // Проверяем просроченность
        if (now.isAfter(deadline)) {
            return "OVERDUE";
        } else {
            // Если не просрочена, возвращаем к исходному статусу (но не OVERDUE)
            return "NOT_STARTED".equals(currentStatus) || "IN_PROGRESS".equals(currentStatus) ?
                    currentStatus : "NOT_STARTED";
        }
    }
}
