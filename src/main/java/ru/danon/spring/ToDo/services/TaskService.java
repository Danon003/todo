package ru.danon.spring.ToDo.services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.danon.spring.ToDo.dto.*;
import ru.danon.spring.ToDo.models.*;
import ru.danon.spring.ToDo.models.id.TaskAssignmentId;
import ru.danon.spring.ToDo.repositories.jpa.TaskAssignmentRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskTagRepository;

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
    private final MLClient mlClient;
    private final TagService tagService;
    private final TaskFileService taskFileService;
    private final FileStorageService fileStorageService;


    @Autowired
    public TaskService(PeopleService peopleService, GroupService groupService, TaskRepository taskRepository, TaskTagRepository taskTagRepository, TaskAssignmentRepository taskAssignmentRepository, NotificationProducerService notificationProducerService, ModelMapper modelMapper, MLClient mlClient, TagService tagService, TaskFileService taskFileService, FileStorageService fileStorageService) {
        this.peopleService = peopleService;
        this.groupService = groupService;
        this.taskRepository = taskRepository;
        this.taskTagRepository = taskTagRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.notificationProducerService = notificationProducerService;
        this.modelMapper = modelMapper;

        this.mlClient = mlClient;
        this.tagService = tagService;
        this.taskFileService = taskFileService;
        this.fileStorageService = fileStorageService;
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

        Task savedTask = taskRepository.save(task);
        taskRepository.flush();

        // Добавляем теги
        if (taskDTO.getTagIds() != null && !taskDTO.getTagIds().isEmpty()) {
            System.out.println("Добавляем теги по ID: " + taskDTO.getTagIds());
            for (Integer tagId : taskDTO.getTagIds()) {
                try {
                    tagService.addTagToTask(savedTask.getId(), tagId);
                    System.out.println("Добавлен тег с ID: " + tagId + " к задаче " + savedTask.getId());
                } catch (Exception e) {
                    System.err.println("Ошибка при добавлении тега с ID " + tagId + ": " + e.getMessage());
                }
            }
        }

        // Добавляем новые теги по имени
        if (taskDTO.getTagNames() != null && !taskDTO.getTagNames().isEmpty()) {
            System.out.println("Добавляем теги по имени: " + taskDTO.getTagNames());
            for (String tagName : taskDTO.getTagNames()) {
                try {
                    tagService.addTagToTaskByName(savedTask.getId(), tagName.trim());
                    System.out.println("Добавлен тег с именем: " + tagName + " к задаче " + savedTask.getId());
                } catch (Exception e) {
                    System.err.println("Ошибка при добавлении тега с именем '" + tagName + "': " + e.getMessage());
                }
            }
        }
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
        taskAssignment.setUpdated_At(LocalDateTime.now());

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

        return taskAssignmentRepository.findByUser(user)
                .stream()
                .map(assignment -> {
                    Task task = assignment.getTask();

                    List<Tag> taskTags = tagService.getTaskTags(task.getId());
                    List<TagDTO> tags = new ArrayList<>();
                    for (Tag tag : taskTags) {
                        tags.add(convertToTagDTO(tag));
                    }

                    return new MyTaskDTO(
                            task.getId(),
                            task.getTitle(),
                            task.getDescription(),
                            task.getDeadline(),
                            task.getPriority(),
                            task.getAuthor() != null ? task.getAuthor().getId() : null,
                            assignment.getStatus(),
                            tags
                    );
                })
                .collect(Collectors.toList());
    }


    public List<MyTaskDTO> findUserTasks(Integer userId) {
        Person user = peopleService.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<TaskAssignment> assignments = taskAssignmentRepository.findByUser(user);

        return assignments.stream().map(assignment -> {
            Task task = assignment.getTask();
            String status = assignment.getStatus();
            Integer authorId = task.getAuthor() != null ? task.getAuthor().getId() : null;

            List<Tag> taskTags = tagService.getTaskTags(task.getId());
            List<TagDTO> tags = new ArrayList<>();
            for (Tag tag : taskTags) {
                tags.add(convertToTagDTO(tag));
            }

            return new MyTaskDTO(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getDeadline(),
                    task.getPriority(),
                    authorId,
                    status,
                    tags
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

        List<Tag> taskTags = tagService.getTaskTags(taskId);
        List<TagDTO> tags = new ArrayList<>();
        for (Tag tag : taskTags) {
            tags.add(convertToTagDTO(tag));
        }

        return new MyTaskDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDeadline(),
                task.getPriority(),
                authorId,
                status,
                tags
        );
    }


    //юзер получает статус конкретной таски
    public StatusDTO findStatusMyTask(Integer taskId, String currentUsername) {
        Integer myId = peopleService.findByUsername(currentUsername).get().getId();
        TaskAssignmentId id = new TaskAssignmentId(taskId, myId);
        TaskAssignment assignment = taskAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found or not assigned to you"));

        Task task = assignment.getTask();
        String status = assignment.getStatus();

        return new StatusDTO(status);
    }

    //юзер меняет статус конкретной таски на переданный status
    @Transactional
    public MyTaskDTO changeMyTask(Integer taskId, String status, String currentUsername) {
        Integer myId = peopleService.findByUsername(currentUsername).get().getId();
        TaskAssignmentId id = new TaskAssignmentId(taskId, myId);

        TaskAssignment assignment = taskAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found or not assigned to you"));

        assignment.setStatus(status);
        assignment.setUpdated_At(LocalDateTime.now());
        taskAssignmentRepository.save(assignment);


        Task task = assignment.getTask();
        Integer authorId = task.getAuthor() != null ? task.getAuthor().getId() : null;

        List<TagDTO> tags = tagService.getTaskTags(task.getId()).stream()
                .map(this::convertToTagDTO)
                .collect(Collectors.toList());

        return new MyTaskDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDeadline(),
                task.getPriority(),
                authorId,
                status,
                tags
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
                    .filter(person -> taskAssignmentRepository.existsById(
                            new TaskAssignmentId(taskId, person.getId())
                    )).map(
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
    private TaskResponseDTO convertToResponseDTO(Task task){

        return modelMapper.map(task, TaskResponseDTO.class);
    }

    private TagDTO convertToTagDTO(Tag tag){
        if (tag == null) return null;
        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        return dto;    }
    private PersonResponseDTO convertToPersonDTO(Person user) {
        return modelMapper.map(user, PersonResponseDTO.class);
    }

    @Transactional
    public Task updateTask(Integer taskId, TaskDTO task, String username) {
        Person person = peopleService.findByUsername(username).orElseThrow(
                () -> new RuntimeException("User not found"));

        Task oldTask = taskRepository.findById(taskId).orElseThrow(
                () -> new RuntimeException("Task not found"));

        if (oldTask.getAuthor() == null) {
            throw new RuntimeException("Task has no author - cannot determine permissions");
        }

        if(!person.getId().equals(oldTask.getAuthor().getId())){
            throw new RuntimeException("You can only edit your own tasks");
        }
        LocalDateTime oldDeadline = oldTask.getDeadline();

        oldTask.setTitle(task.getTitle());
        oldTask.setDescription(task.getDescription());
        oldTask.setDeadline(task.getDeadline());
        oldTask.setPriority(task.getPriority());

        Task updatedTask = taskRepository.save(oldTask);
        updateTaskTags(updatedTask, task);

        if (!oldDeadline.equals(task.getDeadline())) {
            updateTaskAssignmentsStatus(updatedTask);
        }

        return updatedTask;
    }



private void updateTaskTags(Task oldTask, TaskDTO newTask) {
    // Получаем текущие теги задачи - используем ID старой задачи
    List<Tag> currentTags = tagService.getTaskTags(oldTask.getId()); // ← ИСПРАВЛЕНО
    Set<Integer> currentTagIds = currentTags.stream()
            .map(Tag::getId)
            .collect(Collectors.toSet());

    // Обрабатываем теги по ID
    if (newTask.getTagIds() != null && !newTask.getTagIds().isEmpty()) {
        System.out.println("Обрабатываем теги по ID...");
        Set<Integer> newTagIds = new HashSet<>(newTask.getTagIds());

        // Удаляем теги, которых нет в новом списке - используем ID старой задачи
        for (Tag currentTag : currentTags) {
            if (!newTagIds.contains(currentTag.getId())) {
                try {
                    System.out.println("Удаляем тег: " + currentTag.getId() + " - " + currentTag.getName());
                    tagService.removeTagFromTask(oldTask.getId(), currentTag.getId()); // ← ИСПРАВЛЕНО
                } catch (Exception e) {
                    System.err.println("Ошибка при удалении тега " + currentTag.getId() + ": " + e.getMessage());
                }
            }
        }

        // Добавляем новые теги - используем ID старой задачи
        for (Integer tagId : newTask.getTagIds()) {
            if (!currentTagIds.contains(tagId)) {
                try {
                    System.out.println("Добавляем тег по ID: " + tagId);
                    tagService.addTagToTask(oldTask.getId(), tagId); // ← ИСПРАВЛЕНО
                } catch (Exception e) {
                    System.err.println("Ошибка при добавлении тега " + tagId + ": " + e.getMessage());
                }
            }
        }
    }

    // Добавляем новые теги по имени - используем ID старой задачи
    if (newTask.getTagNames() != null && !newTask.getTagNames().isEmpty()) {
        for (String tagName : newTask.getTagNames()) {
            try {
                tagService.addTagToTaskByName(oldTask.getId(), tagName.trim());
            } catch (Exception e) {
                System.err.println("Ошибка при добавлении тега '" + tagName + "': " + e.getMessage());
            }
        }
    }
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
        List<TagDTO> tagDTOs = tagService.getTaskTags(task.getId())
                .stream()
                .map(tag -> new TagDTO(tag.getId(), tag.getName()))
                .collect(Collectors.toList());
        dto.setTags(tagDTOs);

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
                System.out.println("Обновлен статус назначения для пользователя " +
                        assignment.getUserId() + ": " + assignment.getStatus());
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

    /**
     * Загружает решение для задачи (студент)
     */
    @Transactional
    public void uploadSolution(Integer taskId, MultipartFile file, String username) {
        Person student = peopleService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Студент не найден"));

        // Находим назначение
        TaskAssignment assignment = taskAssignmentRepository
                .findById(new TaskAssignmentId(taskId, student.getId()))
                .orElseThrow(() -> new RuntimeException("Задача не назначена студенту"));

        // Проверяем дедлайн
        if (!assignment.canUploadSolution()) {
            throw new RuntimeException("Нельзя загрузить решение после дедлайна");
        }

        // Если уже есть решение - удаляем старое
        if (assignment.hasSolution()) {
            fileStorageService.deleteFile(assignment.getSolutionFilePath());
        }

        // Генерируем путь для нового файла
        String storedFileName = fileStorageService.generateFileName(file.getOriginalFilename());
        String filePath = String.format("tasks/%d/solutions/%d/%s",
                taskId, student.getId(), storedFileName);

        // Загружаем в MinIO
        fileStorageService.uploadFile(file, filePath);

        // Обновляем назначение
        assignment.setSolutionFileName(file.getOriginalFilename());
        assignment.setSolutionFilePath(filePath);
        assignment.setSolutionFileSize(file.getSize());
        assignment.setSolutionUploadedAt(LocalDateTime.now());
        assignment.setStatus("COMPLETED");

        taskAssignmentRepository.save(assignment);

        try {
            Person teacher = assignment.getTask().getAuthor();
            notificationProducerService.sendSolutionUploadedNotification(
                    teacher.getId(),
                    teacher.getRole(),
                    student.getUsername(),
                    assignment.getTask().getTitle(),
                    taskId
            );
        } catch (Exception e) {
            System.out.println("Не удалось отправить уведомление о загрузке решения " + e);
        }
    }

    /**
     * Ставит оценку за решение (преподаватель)
     */
    @Transactional
    public void gradeSolution(Integer taskId, Integer studentId, Integer grade, String comment, String username) {
        Person teacher = peopleService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        // Проверяем что преподаватель имеет право оценивать эту задачу
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Задача не найдена"));

        if (!task.getAuthor().getId().equals(teacher.getId())) {
            throw new RuntimeException("Можно оценивать только свои задачи");
        }

        if(grade > 100)
            throw new RuntimeException("Grade must be less than 100");
        else if(grade < 0)
            throw new RuntimeException("Grade must be greater than 0");


        TaskAssignment assignment = taskAssignmentRepository
                .findById(new TaskAssignmentId(taskId, studentId))
                .orElseThrow(() -> new RuntimeException("Назначение не найдено"));

        assignment.setGrade(grade);
        assignment.setTeacherComment(comment);

        taskAssignmentRepository.save(assignment);

        try {
            Person student = assignment.getUser();

            notificationProducerService.sendSolutionGradedNotification(
                    student.getId(),
                    student.getRole(),
                    teacher.getUsername(),
                    assignment.getTask().getTitle(),
                    grade,
                    comment,
                    taskId
            );
        } catch (Exception e) {
            System.out.println("Не удалось отправить уведомление об оценке решения " + e);
        }
    }

    /**
     * Получает файлы условия задачи
     */
    public List<TaskFile> getTaskFiles(Integer taskId) {
        return taskFileService.getTaskFiles(taskId);
    }

    /**
     * Получает ссылку для скачивания решения
     */
    public String getSolutionDownloadUrl(Integer taskId, String username) {
        Person student = peopleService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Студент не найден"));

        // Находим назначение
        TaskAssignment assignment = taskAssignmentRepository
                .findById(new TaskAssignmentId(taskId, student.getId()))
                .orElseThrow(() -> new RuntimeException("Задача не назначена студенту"));

        if (!assignment.hasSolution()) {
            throw new RuntimeException("Решение не найдено");
        }

        // Генерируем ссылку для скачивания
        return fileStorageService.generateDownloadUrl(assignment.getSolutionFilePath());
    }

    /**
     * Удаляет решение студента
     */
    @Transactional
    public void deleteSolution(Integer taskId, String username) {
        Person student = peopleService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Студент не найден"));

        // Находим назначение
        TaskAssignment assignment = taskAssignmentRepository
                .findById(new TaskAssignmentId(taskId, student.getId()))
                .orElseThrow(() -> new RuntimeException("Задача не назначена студенту"));

        if (!assignment.hasSolution()) {
            throw new RuntimeException("Решение не найдено");
        }

        // Проверяем дедлайн - можно удалять только до дедлайна
        if (!assignment.canUploadSolution()) {
            throw new RuntimeException("Нельзя удалить решение после дедлайна");
        }

        // Удаляем файл из MinIO
        fileStorageService.deleteFile(assignment.getSolutionFilePath());

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
        taskAssignmentRepository.save(assignment);
    }

    /**
     * Получает решение студента (для преподавателя)
     */
    public SolutionDTO getStudentSolution(Integer taskId, Authentication auth) {

        Person user = peopleService.findByUsername(auth.getName()).orElseThrow(
                () -> new RuntimeException("user not found"));

        TaskAssignment assignment = taskAssignmentRepository
                .findById(new TaskAssignmentId(taskId, user.getId()))
                .orElseThrow(() -> new RuntimeException("Назначение не найдено"));

        if (!assignment.hasSolution()) {
            throw new RuntimeException("Решение не найдено");
        }

        SolutionDTO solutionDTO = new SolutionDTO();
        solutionDTO.setFileName(assignment.getSolutionFileName());
        solutionDTO.setFileSize(assignment.getSolutionFileSize());
        solutionDTO.setUploadedAt(assignment.getSolutionUploadedAt());
        solutionDTO.setDownloadUrl(fileStorageService.generateDownloadUrl(assignment.getSolutionFilePath()));
        solutionDTO.setGrade(assignment.getGrade());
        solutionDTO.setTeacherComment(assignment.getTeacherComment());
        solutionDTO.setCanUpload(assignment.canUploadSolution());

        return solutionDTO;
    }

    public List<SolutionDTO> getAllSolutionsForTask(Integer taskId, String teacherUsername) {
        // Проверка прав преподавателя
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        Person teacher = peopleService.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!task.getAuthor().getId().equals(teacher.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Получаем все назначения для этой задачи
        List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(taskId);

        return assignments.stream()
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
    }

    public String getStudentSolutionDownloadUrl(Integer taskId, Integer studentId, String teacherUsername) {
        TaskAssignment assignment = taskAssignmentRepository.findByUserIdAndTaskId(studentId, taskId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        // Проверка прав преподавателя
        if (!assignment.getTask().getAuthor().getUsername().equals(teacherUsername)) {
            throw new RuntimeException("Access denied");
        }

        if (assignment.getSolutionFilePath() == null) {
            throw new RuntimeException("Решение не найдено");
        }

        return fileStorageService.generateDownloadUrl(assignment.getSolutionFilePath());
    }
}

