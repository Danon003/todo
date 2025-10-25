package ru.danon.spring.ToDo.services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.DashboardStatsDTO;
import ru.danon.spring.ToDo.dto.LogResponseDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.models.*;
import ru.danon.spring.ToDo.repositories.jpa.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final PeopleService peopleService;
    private final GroupRepository groupRepository;
    private final NotificationProducerService notificationProducerService;
    private final RoleAuditLogRepository roleAuditLogRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupService groupService;
    private final ModelMapper modelMapper;

    @Autowired
    public AdminService(PeopleService peopleService, GroupRepository groupRepository, NotificationProducerService notificationProducerService, RoleAuditLogRepository roleAuditLogRepository, TaskRepository taskRepository, TaskService taskService, TaskAssignmentRepository taskAssignmentRepository, UserGroupRepository userGroupRepository, GroupService groupService, ModelMapper modelMapper) {
        this.peopleService = peopleService;
        this.groupRepository = groupRepository;
        this.notificationProducerService = notificationProducerService;
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.roleAuditLogRepository = roleAuditLogRepository;


        this.taskAssignmentRepository = taskAssignmentRepository;
        this.userGroupRepository = userGroupRepository;
        this.groupService = groupService;
        this.modelMapper = modelMapper;
    }

    public List<Person> getAllUsers() {
        return peopleService.findAll();
    }

    public void createGroup(String groupName, String description) {

        Group group = new Group();
        group.setName(groupName);
        group.setDescription(description);
        group.setCreatedAt(LocalDateTime.now());
        group.setTeacher(null);

        groupRepository.save(group);
    }

    @Transactional
    public void changeUserRole(Integer userId, String newRole) {
        Person user = peopleService.findById(userId).orElseThrow(
                () -> new RuntimeException("User not found"));

        RoleAuditLog log = new RoleAuditLog();
        log.setUser(user);
        log.setOldRole(user.getRole());
        log.setNewRole(newRole);
        log.setChangedAt(LocalDateTime.now());

        // Обработка для TEACHER
        if(user.getRole().equals("ROLE_TEACHER")){
            List<Group> groups = groupRepository.findByTeacherId(userId);
            for(Group group : groups) {
                group.setTeacher(null);
                groupRepository.save(group);
            }
        }

        if(user.getRole().equals("ROLE_STUDENT")){
            Integer groupId = groupService.getUserGroup(user.getUsername());
            if (groupId != null) {
                groupService.removeStudentFromGroup(groupId, userId);
            }
        }

        user.setRole(newRole);
        peopleService.save(user);

        //уведомление: вам назначили новую роль
        notificationProducerService.sendChangeRoleNotification(
                userId,
                newRole
        );

        roleAuditLogRepository.save(log);
    }

    @Transactional
    public void assignTeacherToGroup(Integer groupId, Integer userId) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found with id"));
        Person user = peopleService.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Person previousTeacher = group.getTeacher();

        group.setTeacher(user);

        groupRepository.save(group);

        if (previousTeacher != null && !previousTeacher.getId().equals(userId)) {
            notificationProducerService.sendTeacherRemovedNotification(
                    previousTeacher.getId(),
                    group.getName()
            );
        }

        notificationProducerService.sendTeacherAssignNotification(
                user.getId(),
                group.getName()
        );
    }

    @Transactional
    public Person createTeacher(Integer id) {
        Person teacher = peopleService.findById(id).orElseThrow(
                () -> new RuntimeException("User not found"));

        RoleAuditLog log = new RoleAuditLog();
        log.setUser(teacher);
        log.setOldRole(teacher.getRole());
        log.setNewRole("ROLE_TEACHER");
        log.setChangedAt(LocalDateTime.now());


        teacher.setRole("ROLE_TEACHER");
        teacher.setCreatedAt(LocalDateTime.now());

        notificationProducerService.sendChangeRoleNotification(
                id,
                "TEACHER"
        );

        roleAuditLogRepository.save(log);

        return peopleService.save(teacher);
    }

    public List<Person> getUsersByRole(String role) {
        return peopleService.findByRole("ROLE_" + role);
    }

    public List<LogResponseDTO> getRoleAuditLogs() {
        return convertToLogResponse(roleAuditLogRepository.findAll());
    }



    public DashboardStatsDTO getDashboardStats(Authentication auth) {
        String username = auth.getName();
        PersonResponseDTO userInfo = peopleService.getUserInfo(username);
        String role = auth.getAuthorities().iterator().next().getAuthority();

        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setUserInfo(userInfo);

        switch (role) {
            case "ROLE_ADMIN":
                // Общее количество пользователей
                stats.setTotalUsers(peopleService.findAll().size());

                // Общее количество групп
                stats.setTotalGroups(groupRepository.findAll().size());

                // Общее количество задач (всех задач в системе)
                stats.setTotalTasks(taskService.findAllTasks().size());

                // Статистика по ролям
                Map<String, Integer> roleStats = new HashMap<>();
                for (Person person : peopleService.findAll()) {
                    String userRole = person.getRole();
                    roleStats.put(userRole, roleStats.getOrDefault(userRole, 0) + 1);
                }
                stats.setRoleStatistics(roleStats);
                break;

            case "ROLE_TEACHER":
                Person teacher = peopleService.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("Teacher not found"));

                // Группы преподавателя
                List<Group> teacherGroups = groupRepository.findByTeacherId(teacher.getId());
                stats.setTotalGroups(teacherGroups.size());

                // Студенты преподавателя (все студенты из его групп)
                List<Person> teacherStudents = groupService.findByTeacherId(auth);
                stats.setTotalStudents(teacherStudents.size());

                // Задачи преподавателя
                List<Task> teacherTasks = taskRepository.findByAuthorId(teacher.getId());
                stats.setMyCreatedTasks(teacherTasks.size());

                // 1. Средний прогресс студентов
                double totalStudentProgress = 0;
                int studentsWithTasksCount = 0;

                // 2. Нагрузка (мин/макс/среднее)
                int totalStudentTasks = 0;
                int minTasks = Integer.MAX_VALUE;
                int maxTasks = 0;

                for (Person student : teacherStudents) {
                    // Задачи студента от этого препода
                    List<TaskAssignment> studentAssignments = taskAssignmentRepository.findByUserId(student.getId());
                    int studentTaskCount = studentAssignments.size();

                    if (studentTaskCount > 0) {
                        // Прогресс студента
                        long completedCount = studentAssignments.stream()
                                .filter(assignment -> "COMPLETED".equals(assignment.getStatus()))
                                .count();
                        double studentProgress = (double) completedCount / studentTaskCount * 100;
                        totalStudentProgress += studentProgress;
                        studentsWithTasksCount++;

                        // Нагрузка
                        totalStudentTasks += studentTaskCount;
                        minTasks = Math.min(minTasks, studentTaskCount);
                        maxTasks = Math.max(maxTasks, studentTaskCount);
                    }
                }

                // Средний прогресс
                if (studentsWithTasksCount > 0) {
                    stats.setAvgStudentProgress(totalStudentProgress / studentsWithTasksCount);
                } else {
                    stats.setAvgStudentProgress(0.0);
                }

                // Нагрузка
                if (studentsWithTasksCount > 0) {
                    stats.setAvgTasksPerStudent((double) totalStudentTasks / studentsWithTasksCount);
                    stats.setMinTasks(minTasks == Integer.MAX_VALUE ? 0 : minTasks);
                    stats.setMaxTasks(maxTasks);
                } else {
                    stats.setAvgTasksPerStudent(0.0);
                    stats.setMinTasks(0);
                    stats.setMaxTasks(0);
                }

                // 3. ⏰ Просрочки
                int totalOverdueTasks = 0;
                for (Person student : teacherStudents) {
                    List<TaskAssignment> studentAssignments = taskAssignmentRepository.findByUserId(student.getId());
                    long studentOverdueCount = studentAssignments.stream()
                            .filter(assignment -> {
                                Task task = assignment.getTask();
                                // Добавляем проверку на null для task и task.getAuthor()
                                return task != null &&
                                        task.getAuthor() != null &&
                                        task.getAuthor().getId().equals(teacher.getId()) && // задача препода
                                        task.getDeadline() != null &&
                                        task.getDeadline().isBefore(LocalDateTime.now()) && // просрочена
                                        !"COMPLETED".equals(assignment.getStatus()); // не выполнена
                            })
                            .count();
                    totalOverdueTasks += (int) studentOverdueCount;
                }
                stats.setTotalOverdueTasks(totalOverdueTasks);

                // 4. 🐢 Стагнация
                LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);
                List<TaskAssignment> stuckAssignments = taskAssignmentRepository.findStuckByTeacherId(teacher.getId(), twoWeeksAgo);
                stats.setStuckTasks(stuckAssignments.size());

                // 5. 👨‍🏫 Задачи в группах препода
                long tasksInTeacherGroups = teacherTasks.stream()
                        .filter(task -> !taskAssignmentRepository.findByTaskId(task.getId()).isEmpty())
                        .count();
                stats.setTasksAssignedToMyGroups((int) tasksInTeacherGroups);
                long totalSystemTasks = taskRepository.count();
                stats.setTotalTasks((int) totalSystemTasks);

                break;

            case "ROLE_STUDENT":
                Person student = peopleService.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("Student not found"));

                // Получаем задачи студента через назначения
                List<TaskAssignment> studentAssignments = taskAssignmentRepository.findByUserId(student.getId());

                int activeStudentTasks = 0;
                int completedStudentTasks = 0;
                Map<String, Integer> statusCount = new HashMap<>();
                LocalDateTime nextDeadline = null;

                for (TaskAssignment assignment : studentAssignments) {
                    String status = assignment.getStatus();

                    // Считаем статусы
                    statusCount.put(status, statusCount.getOrDefault(status, 0) + 1);

                    // Считаем активные и завершенные
                    if ("COMPLETED".equals(status)) {
                        completedStudentTasks++;
                    }
                    else {
                        activeStudentTasks++;

                        // Ищем ближайший дедлайн для незавершенных задач
                        Task task = assignment.getTask();
                        if (task != null && task.getDeadline() != null && !"OVERDUE".equals(status)) {
                            if (nextDeadline == null || task.getDeadline().isBefore(nextDeadline)) {
                                nextDeadline = task.getDeadline();
                            }
                        }
                    }
                }

                stats.setActiveTasks(activeStudentTasks - statusCount.getOrDefault("OVERDUE", 0));
                stats.setCompletedTasks(completedStudentTasks);
                stats.setStatusCount(statusCount);
                stats.setNextDeadline(nextDeadline);
                break;
        }

        return stats;
    }
    private List<LogResponseDTO> convertToLogResponse(List<RoleAuditLog> all) {
        return all.stream()
                .map(log -> modelMapper.map(log, LogResponseDTO.class))
                .collect(Collectors.toList());
    }
}

