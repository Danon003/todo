package ru.danon.spring.ToDo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.DashboardStatsDTO;
import ru.danon.spring.ToDo.dto.MyTaskDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.dto.StatisticDTO;
import ru.danon.spring.ToDo.models.*;
import ru.danon.spring.ToDo.repositories.*;

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

    @Autowired
    public AdminService(PeopleService peopleService, GroupRepository groupRepository, NotificationProducerService notificationProducerService, RoleAuditLogRepository roleAuditLogRepository, TaskRepository taskRepository, TaskService taskService, TaskAssignmentRepository taskAssignmentRepository, UserGroupRepository userGroupRepository) {
        this.peopleService = peopleService;
        this.groupRepository = groupRepository;
        this.notificationProducerService = notificationProducerService;
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.roleAuditLogRepository = roleAuditLogRepository;


        this.taskAssignmentRepository = taskAssignmentRepository;
        this.userGroupRepository = userGroupRepository;
    }

    public List<Person> getAllUsers() {
        return peopleService.findAll();
    }

    public void createGroup(String groupName, String description) {

        Group group = new Group();
        group.setName(groupName);
        group.setDescription(description);
        group.setCreatedAt(LocalDateTime.now());

        groupRepository.save(group);
    }

    public void changeUserRole(Integer userId, String newRole) {
        Person user = peopleService.findById(userId).orElseThrow(
                () -> new RuntimeException("User not found"));

        RoleAuditLog log = new RoleAuditLog();
        log.setUser(user);
        log.setOldRole(user.getRole());
        log.setNewRole(newRole);
        log.setChangedAt(LocalDateTime.now());

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

    public List<RoleAuditLog> getRoleAuditLogs() {
        return roleAuditLogRepository.findAll();
    }

    public StatisticDTO getStatistic(Authentication auth) {
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_TEACHER"))) {
            return new StatisticDTO(
                    peopleService.getUserInfo(auth.getName()),
                    groupRepository.findAll().size(),
                    peopleService.findAll().size(),
                    taskService.findAllTasks().size()
            );
        } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return new StatisticDTO(
                    peopleService.getUserInfo(auth.getName()),
                    groupRepository.findAll().size(),
                    peopleService.findAll().size(),
                    taskService.findAllTasks().size()
            );
        } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STUDENT"))) {
            return new StatisticDTO(
                    peopleService.getUserInfo(auth.getName()),
                    0,
                    0,
                    taskService.findMyTasks(auth.getName()).size()
            );
        }
        return null;
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

                // Активные задачи (все назначения где статус не COMPLETED)
                int activeTasks = 0;
                List<Task> allTasks = taskService.findAllTasks();
                for (Task task : allTasks) {
                    // Для каждой задачи получаем все назначения
                    List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(task.getId());
                    for (TaskAssignment assignment : assignments) {
                        if (!"COMPLETED".equals(assignment.getStatus())) {
                            activeTasks++;
                        }
                    }
                }
                stats.setActiveTasks(activeTasks);
                break;

            case "ROLE_TEACHER":
                Person teacher = peopleService.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("Teacher not found"));

                // Группы преподавателя (где он является создателем)


                // Общее количество студентов в группах преподавателя


                // Статистика задач - все задачи, созданные преподавателем
                int teacherTasksCount = 0;
                int completedTeacherTasks = 0;

                List<Task> allTeacherTasks = taskRepository.findByAuthorId(teacher.getId());
                for (Task task : allTeacherTasks) {
                    teacherTasksCount++;
                    // Проверяем статусы всех назначений этой задачи
                    List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(task.getId());
                    boolean allCompleted = true;
                    for (TaskAssignment assignment : assignments) {
                        if (!"COMPLETED".equals(assignment.getStatus())) {
                            allCompleted = false;
                            break;
                        }
                    }
                    if (allCompleted && !assignments.isEmpty()) {
                        completedTeacherTasks++;
                    }
                }

                stats.setTotalAssignedTasks(teacherTasksCount);
                stats.setCompletedTasks(completedTeacherTasks);
                stats.setActiveTasks(teacherTasksCount - completedTeacherTasks);
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
                    } else {
                        activeStudentTasks++;

                        // Ищем ближайший дедлайн для незавершенных задач
                        Task task = assignment.getTask();
                        if (task != null && task.getDeadline() != null) {
                            if (nextDeadline == null || task.getDeadline().isBefore(nextDeadline)) {
                                nextDeadline = task.getDeadline();
                            }
                        }
                    }
                }

                stats.setActiveTasks(activeStudentTasks);
                stats.setCompletedTasks(completedStudentTasks);
                stats.setStatusCount(statusCount);
                stats.setNextDeadline(nextDeadline);
                break;
        }

        return stats;
    }
}

