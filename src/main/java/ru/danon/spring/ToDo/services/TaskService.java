package ru.danon.spring.ToDo.services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.TaskDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.dto.TaskStatDTO;
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
    private final TagRepository tagRepository;
    private final TaskTagRepository taskTagRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final ModelMapper modelMapper;


    @Autowired
    public TaskService(PeopleService peopleService, GroupService groupService, TaskRepository taskRepository, TagRepository tagRepository, TaskTagRepository taskTagRepository, TaskAssignmentRepository taskAssignmentRepository, ModelMapper modelMapper) {
        this.peopleService = peopleService;
        this.groupService = groupService;
        this.taskRepository = taskRepository;
        this.tagRepository = tagRepository;
        this.taskTagRepository = taskTagRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.modelMapper = modelMapper;

    }

    @Transactional
    public TaskResponseDTO createTask(TaskDTO taskDTO, String username) {

        Person author = peopleService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Author not found"));

        Task task = new Task();
        task.setTitle(taskDTO.getTitle());
        task.setDescription(taskDTO.getDescription());
        task.setDeadline(taskDTO.getDeadline());
        task.setPriority(taskDTO.getPriority());
        task.setAuthor(author);
        task.setStatus("NOT_STARTED");
        task.setCreatedAt(LocalDateTime.now());


        Task savedTask = taskRepository.save(task);

        // Добавляем теги к задаче
        if (taskDTO.getTagIds() != null) {
            for (Integer tagId : taskDTO.getTagIds()) {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new RuntimeException("Tag not found"));
                addTagToTask(savedTask.getId(), tagId);
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

        taskAssignmentRepository.save(taskAssignment);
    }

    //назначить таску группе по её Id (функция для препода)
    @Transactional
    public void assignTaskForGroup(Integer taskID, Integer groupId, String currentUsername) {
        List<Person> groupMembers = groupService.getPersonsByGroupId(groupId);
        for (Person groupMember : groupMembers) {
            assignTask(taskID, groupMember.getId(), currentUsername);
        }
    }

    //получить статус таски (функция для препода)
    public TaskStatDTO findStatusTask(Integer id, Integer taskId, String filter) {
        TaskStatDTO taskDTO = new TaskStatDTO();

        if ("student".equalsIgnoreCase(filter)) {
            TaskAssignment assignment = taskAssignmentRepository.findByUserIdAndTaskId(id, taskId)
                    .orElseThrow(() -> new RuntimeException("Task not assigned to user"));

            taskDTO.setStatus(assignment.getTask().getStatus());
            taskDTO.setUserId(id);
        }
        else if ("group".equalsIgnoreCase(filter)) {
            List<TaskAssignment> assignments = taskAssignmentRepository.findByGroupIdAndTaskId(id, taskId);

            Map<String, Integer> statusStatistics = new HashMap<>();
            for (TaskStatus status : TaskStatus.values()) {
                statusStatistics.put(status.name(), 0);
            }

            assignments.forEach(assignment -> {
                String status = assignment.getTask().getStatus();
                statusStatistics.put(status, statusStatistics.get(status) + 1);
            });

            taskDTO.setStatusStatistics(statusStatistics);
            taskDTO.setGroupId(id);
        }
        else {
            throw new RuntimeException("Invalid filter value. Use 'student' or 'group'");
        }

        taskDTO.setId(taskId);
        return taskDTO;
    }

    //юзер ищет свои таски
    public List<Task> findMyTasks(String currentUsername) {

        Person assignedBy = peopleService.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Current user not found in DB"));

        return taskAssignmentRepository.findByUser(assignedBy)
                .stream()
                .map(TaskAssignment::getTask)
                .collect(Collectors.toList());
    }

    //юзер ищет свою конкретную таску
    public Task findMyTasksById(Integer taskId, String currentUsername) {
        Integer myId = peopleService.findByUsername(currentUsername).get().getId();
        TaskAssignmentId id = new TaskAssignmentId(taskId, myId);

        return taskAssignmentRepository.findById(id)
                .map(TaskAssignment::getTask)
                .orElseThrow(() -> new RuntimeException("Task not found or not assigned to you"));
    }

    //юзер получает статус конкретной таски
    public Task findStatusMyTask(Integer taskId, String currentUsername) {
        Integer myId = peopleService.findByUsername(currentUsername).get().getId();
        TaskAssignmentId id = new TaskAssignmentId(taskId, myId);
        TaskAssignment assignment = taskAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found or not assigned to you"));

        Task task = assignment.getTask();
        return task;
    }

    //юзер меняет статус конкретной таски на переданный status
    @Transactional
    public Task changeMyTask(Integer taskId, String status, String currentUsername) {
        Integer myId = peopleService.findByUsername(currentUsername).get().getId();
        TaskAssignmentId id = new TaskAssignmentId(taskId, myId);
        TaskAssignment assignment = taskAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TaskAssignment not found"));

        Task task = assignment.getTask();
        task.setStatus(status);

        return taskRepository.save(task);
    }

    //юзер делится таской с другим юзером
    @Transactional
    public void shareTask(Integer taskId, Integer userId, String currentUsername) {
        //сделать проверку (1.У отправителя должна быть эта задача, иначе он не сможет ей делиться,
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

    @Transactional
    public void addTagToTask(Integer taskId, Integer tagId) {
        if (taskTagRepository.existsById(new TaskTagId(taskId, tagId))) {
            throw new RuntimeException("Task tag already exists");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("Tag not found"));

        TaskTag taskTag = new TaskTag();
        taskTag.setTaskId(taskId);
        taskTag.setTagId(tagId);
        taskTag.setTask(task);
        taskTag.setTag(tag);

        taskTagRepository.save(taskTag);
    }

    @Transactional
    public void removeTagFromTask(Integer taskId, Integer tagId) {
        taskTagRepository.deleteById(new TaskTagId(taskId, tagId));
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


}

