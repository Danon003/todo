package ru.danon.spring.ToDo.services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.*;
import ru.danon.spring.ToDo.models.*;
import ru.danon.spring.ToDo.models.id.TaskAssignmentId;
import ru.danon.spring.ToDo.models.id.TaskTagId;
import ru.danon.spring.ToDo.repositories.TagRepository;
import ru.danon.spring.ToDo.repositories.TaskAssignmentRepository;
import ru.danon.spring.ToDo.repositories.TaskRepository;
import ru.danon.spring.ToDo.repositories.TaskTagRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final PeopleService peopleService;
    private final GroupService groupService;
    private final TaskRepository taskRepository;
    private final TaskTagRepository taskTagRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final NotificationProducerService notificationProducerService;
    private final ModelMapper modelMapper;


    @Autowired
    public TaskService(PeopleService peopleService, GroupService groupService, TaskRepository taskRepository, TaskTagRepository taskTagRepository, TaskAssignmentRepository taskAssignmentRepository, NotificationProducerService notificationProducerService, ModelMapper modelMapper) {
        this.peopleService = peopleService;
        this.groupService = groupService;
        this.taskRepository = taskRepository;
        this.taskTagRepository = taskTagRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.notificationProducerService = notificationProducerService;
        this.modelMapper = modelMapper;

    }

    @Transactional
    public TaskResponseDTO createTask(MyTaskDTO taskDTO, String username) {

        Person author = peopleService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Author not found"));

        Task task = new Task();
        task.setTitle(taskDTO.getTitle());
        task.setDescription(taskDTO.getDescription());
        task.setDeadline(taskDTO.getDeadline());
        task.setPriority(taskDTO.getPriority());
        task.setAuthor(author);
        task.setCreatedAt(LocalDateTime.now());

        TaskAssignment taskAssignment = new TaskAssignment();
        taskAssignment.setStatus("NOT_STARTED");

        Task savedTask = taskRepository.save(task);

        return convertToResponseDTO(savedTask);
    }

    //удалить таску
    @Transactional
    public void deleteTask(Integer taskId) {
        taskAssignmentRepository.deleteByTaskId(taskId);
        taskTagRepository.deleteByTaskId(taskId);
        taskRepository.deleteById(taskId);
    }

    //просмотреть все созданные таски
    public List<Task> findAllTasks() {
        return taskRepository.findAll();
    }

    //посмотреть конкретную таску
    public Task findTaskById(Integer taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }

    //назначить таску юзеру(функция для препода)
    @Transactional
    public void assignTask(Integer taskId, Integer userId, String currentUsername) {
        if (taskAssignmentRepository.existsById(new TaskAssignmentId(taskId, userId))) {
            throw new RuntimeException("Task assignment already exists");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        Person user = peopleService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Person assignedBy = peopleService.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        if (taskAssignmentRepository.existsByTaskAndUser(task, user)) {
            throw new RuntimeException("Task assignment already exists");
        }

        TaskAssignment taskAssignment = new TaskAssignment();
        taskAssignment.setTaskId(taskId);
        taskAssignment.setUserId(userId);
        taskAssignment.setTask(task);
        taskAssignment.setUser(user);
        taskAssignment.setAssignedBy(assignedBy);
        taskAssignment.setAssignedAt(LocalDateTime.now());

        taskAssignmentRepository.save(taskAssignment);

        //уведомление: вам назначена новая задача
        notificationProducerService.sendTaskAssignedNotification(
                userId,
                user.getRole(),
                task.getTitle(),
                taskId
        );
    }

    //назначить таску группе по её Id (функция для препода)
    @Transactional
    public void assignTaskForGroup(Integer taskID, Integer groupId, String currentUsername) {
        List<Person> groupMembers = groupService.getPersonsByGroupId(groupId);

        // Находим пользователей, которым задача еще не назначена
        List<Person> usersToAssign = groupMembers.stream()
                .filter(user -> !taskAssignmentRepository.existsById(
                        new TaskAssignmentId(taskID, user.getId())))
                .toList();

        // Назначаем задачу только тем, у кого ее еще нет
        for (Person user : usersToAssign) {
            assignTask(taskID, user.getId(), currentUsername);
        }

        System.out.println("Assigned to " + usersToAssign.size() + " users, skipped " +
                (groupMembers.size() - usersToAssign.size()) + " users (already assigned)");
    }

    //получить статус таски (функция для препода)
    public TaskStatDTO findStatusTask(Integer id, Integer taskId, String filter) {
        TaskStatDTO taskDTO = new TaskStatDTO();
        taskDTO.setId(taskId);

        if ("student".equalsIgnoreCase(filter)) {
            // Получаем назначение → статус у него
            TaskAssignment assignment = taskAssignmentRepository.findByUserIdAndTaskId(id, taskId)
                    .orElseThrow(() -> new RuntimeException("Задача не назначена пользователю"));

            taskDTO.setStatus(assignment.getStatus());
            taskDTO.setUserId(id);

        } else if ("group".equalsIgnoreCase(filter)) {
            // Получаем все назначения задачи в группе
            List<TaskAssignment> assignments = taskAssignmentRepository.findByGroupIdAndTaskId(id, taskId);

            // Инициализируем счётчики по всем возможным статусам
            Map<String, Integer> statusStatistics = new HashMap<>();
            for (TaskStatus status : TaskStatus.values()) {
                statusStatistics.put(status.name(), 0);
            }

            // Считаем статусы из назначений
            assignments.forEach(assignment -> {
                String status = assignment.getStatus();
                if (statusStatistics.containsKey(status)) {
                    statusStatistics.put(status, statusStatistics.get(status) + 1);
                } else {
                    // На случай, если статус не в enum (например, OVERDUE)
                    statusStatistics.merge("OTHER", 1, Integer::sum);
                }
            });

            taskDTO.setStatusStatistics(statusStatistics);
            taskDTO.setGroupId(id);

        } else {
            throw new RuntimeException("Неверный фильтр. Используйте 'student' или 'group'");
        }

        return taskDTO;
    }


    public List<MyTaskDTO> findMyTasks(String username) {
        Person user = peopleService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));

        List<TaskAssignment> assignments = taskAssignmentRepository.findByUser(user);

        return assignments.stream().map(assignment -> {
            Task task = assignment.getTask();
            String status = assignment.getStatus();
            Integer authorId = task.getAuthor() != null ? task.getAuthor().getId() : null;
            return new MyTaskDTO(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getDeadline(),
                    task.getPriority(),
                    authorId,
                    status
            );
        }).collect(Collectors.toList());
    }

    public List<MyTaskDTO> findUserTasks(Integer userId) {
        Person user = peopleService.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<TaskAssignment> assignments = taskAssignmentRepository.findByUser(user);

        return assignments.stream().map(assignment -> {
            Task task = assignment.getTask();
            String status = assignment.getStatus();
            Integer authorId = task.getAuthor() != null ? task.getAuthor().getId() : null;
            return new MyTaskDTO(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getDeadline(),
                    task.getPriority(),
                    authorId,
                    status
            );
        }).collect(Collectors.toList());
    }

    //юзер ищет свою конкретную таску
    public MyTaskDTO findMyTasksById(Integer taskId, String currentUsername) {
        Person currentUser = peopleService.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Создаём составной ID
        TaskAssignmentId assignmentId = new TaskAssignmentId(taskId, currentUser.getId());

        // Находим назначение задачи (включает статус!)
        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Task not found or not assigned to you"));

        // Достаём саму задачу и её статус
        Task task = assignment.getTask();
        String status = assignment.getStatus();
        Integer authorId = task.getAuthor() != null ? task.getAuthor().getId() : null;
        return new MyTaskDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDeadline(),
                task.getPriority(),
                authorId,
                status
        );
    }


    //юзер получает статус конкретной таски
    public MyTaskDTO findStatusMyTask(Integer taskId, String currentUsername) {
        Integer myId = peopleService.findByUsername(currentUsername).get().getId();
        TaskAssignmentId id = new TaskAssignmentId(taskId, myId);
        TaskAssignment assignment = taskAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found or not assigned to you"));

        Task task = assignment.getTask();
        String status = assignment.getStatus();
        Integer authorId = task.getAuthor() != null ? task.getAuthor().getId() : null;
        return new MyTaskDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDeadline(),
                task.getPriority(),
                authorId,
                status
        );
    }

    //юзер меняет статус конкретной таски на переданный status
    @Transactional
    public MyTaskDTO changeMyTask(Integer taskId, String status, String currentUsername) {
        Integer myId = peopleService.findByUsername(currentUsername).get().getId();
        TaskAssignmentId id = new TaskAssignmentId(taskId, myId);

        TaskAssignment assignment = taskAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found or not assigned to you"));

        assignment.setStatus(status);  // ← Меняем статус у назначения
        taskAssignmentRepository.save(assignment);


        Task task = assignment.getTask();
        Integer authorId = task.getAuthor() != null ? task.getAuthor().getId() : null;
        return new MyTaskDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDeadline(),
                task.getPriority(),
                authorId,
                status
        );
    }

    //юзер делится таской с другим юзером
    @Transactional
    public void shareTask(Integer taskId, Integer userId, String currentUsername) {
        //1.У отправителя должна быть эта задача, иначе он не сможет ей делиться,
        //2.У получателя не должно быть назначенной этой таски, если она у него есть, вывести сообщение (Пользователь решает эту задачу)

        Person currentPerson = peopleService.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Current user not found in DB"));

        TaskAssignmentId senderId = new TaskAssignmentId(taskId, currentPerson.getId());
        if (!taskAssignmentRepository.existsById(senderId))
            throw new RuntimeException("You don't have this task to share");


        TaskAssignmentId receiverId = new TaskAssignmentId(taskId, userId);
        if (taskAssignmentRepository.existsById(receiverId))
            throw new RuntimeException("User already has this task");


        assignTask(taskId, userId, currentUsername);
    }

    private TaskResponseDTO convertToResponseDTO(Task task){
        return modelMapper.map(task, TaskResponseDTO.class);
    }

    public Set<TaskResponseDTO> getGroupTasks(Integer groupId) {
        List<Person> groupMembers = groupService.getPersonsByGroupId(groupId);

        // Создаем Set для хранения уникальных задач
        Set<TaskResponseDTO> groupTasks = new HashSet<>();

        // Для каждого пользователя в группе получаем его задачи и добавляем в Set
        for (Person member : groupMembers) {
            List<Task> userTasks = taskAssignmentRepository.findByUser(member)
                    .stream()
                    .map(TaskAssignment::getTask)
                    .toList();

            // Конвертируем задачи в DTO и добавляем в Set
            userTasks.forEach(task -> groupTasks.add(convertToResponseDTO(task)));
        }

        return groupTasks;
    }

    @Modifying
    @Scheduled(fixedRate = 120000) //каждые две минуты
    @Transactional
    public void updateOverdueTasks() {
        LocalDateTime now = LocalDateTime.now();

        // Получаем все задания, которые станут просроченными
        List<TaskAssignment> overdueAssignments = taskAssignmentRepository.findOverdueTaskAssignments(now);

        // Обновляем статус
        taskAssignmentRepository.updateOverdueTaskAssignments(now);

        // Отправляем уведомления
        for (TaskAssignment assignment : overdueAssignments) {
            notificationProducerService.sendTaskOverdueNotification(
                    assignment.getUserId(),
                    "ROLE_STUDENT",
                    assignment.getTask().getTitle(),
                    assignment.getTask().getId()
            );
        }
    }

    @Scheduled(fixedRate = 3600000)
    public void checkApproachingDeadlines() {
        LocalDateTime now = LocalDateTime.now();

        checkDeadlineWindow(now, "через 2 дня", "TASK_DEADLINE_2D", 48);   // 48–46 часов
        checkDeadlineWindow(now, "через 1 день", "TASK_DEADLINE_1D", 24);   // 24–22 часа
        checkDeadlineWindow(now, "через 12 часов", "TASK_DEADLINE_12H", 12); // 12–10 часов
    }


    private void checkDeadlineWindow(LocalDateTime now, String label, String eventType, int hoursBefore) {
        LocalDateTime windowStart = now.plusHours(hoursBefore - 1); // начало окна
        LocalDateTime windowEnd = now.plusHours(hoursBefore);       // конец окна

        List<TaskAssignment> assignments = taskAssignmentRepository.findTasksWithDeadlineInWindow(
                windowStart, windowEnd
        );

        for (TaskAssignment assignment : assignments) {
            notificationProducerService.sendTaskDeadlineApproachingNotification(
                    assignment.getUserId(),
                    "ROLE_STUDENT",
                    assignment.getTask().getTitle(),
                    assignment.getTask().getId(),
                    label,
                    eventType
            );
        }
    }

    public List<PersonResponseDTO> getUsersWithTask(Integer taskId, Authentication auth) {
        Person user = peopleService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (groupService.isTeacher(user)) {
            return groupService.findByTeacherId(auth)
                    .stream()
                    .filter(person -> {
                        try {
                            findMyTasksById(taskId, person.getUsername());
                            return true;
                        } catch (RuntimeException e) {
                            return false;
                        }
                    }).map(
                            this::convertToPersonDTO
                    )
                    .toList();
        } else {
            return groupService.getPersonsByGroupId(groupService.getUserGroup(auth.getName()))
                    .stream()
                    .filter(person -> !person.getId().equals(user.getId())) // убираем текущего пользователя
                    .filter(person -> {
                        try {
                            findMyTasksById(taskId, person.getUsername());
                            return true;
                        } catch (RuntimeException e) {
                            return false;
                        }
                    }).map(
                            this::convertToPersonDTO
                    )
                    .toList();
        }
    }
    private PersonResponseDTO convertToPersonDTO(Person user) {
        return modelMapper.map(user, PersonResponseDTO.class);
    }
}

