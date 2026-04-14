package ru.danon.spring.ToDo.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.DashboardStatsDTO;
import ru.danon.spring.ToDo.dto.LogResponseDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.models.postgre.Group;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.models.postgre.RoleAuditLog;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.models.postgre.TaskAssignment;
import ru.danon.spring.ToDo.repositories.jpa.GroupRepository;
import ru.danon.spring.ToDo.repositories.jpa.RoleAuditLogRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskAssignmentRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskRepository;
import ru.danon.spring.ToDo.services.AdminService;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.NotificationProducerService;
import ru.danon.spring.ToDo.services.PeopleService;
import ru.danon.spring.ToDo.services.TaskService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final PeopleService peopleService;
    private final GroupRepository groupRepository;
    private final NotificationProducerService notificationProducerServiceImpl;
    private final RoleAuditLogRepository roleAuditLogRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskServiceImpl;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final GroupService groupServiceImpl;
    private final ModelMapper modelMapper;

    @Override
    public Page<Person> getAllUsers(Pageable page) {
        log.debug("Получение всех пользователей с пагинацией: page={}, size={}",
                page.getPageNumber(), page.getPageSize());
        return peopleService.findAll(page);
    }

    @Transactional
    @Override
    public void createGroup(String groupName, String description) {
        log.info("Создание новой группы: name={}, description={}", groupName, description);

        Group group = new Group();
        group.setName(groupName);
        group.setDescription(description);
        group.setCreatedAt(LocalDateTime.now());
        group.setTeacher(null);

        Group savedGroup = groupRepository.save(group);
        log.info("Группа успешно создана: id={}, name={}", savedGroup.getId(), groupName);
    }

    @Transactional
    @Override
    public void changeUserRole(Integer userId, String newRole) {
        log.info("Изменение роли пользователя id={} на {}", userId, newRole);

        Person user = peopleService.findById(userId).orElseThrow(
                () -> {
                    log.error("Пользователь id={} не найден при попытке смены роли", userId);
                    return new EntityNotFoundException("User not found");
                });

        RoleAuditLog auditLog = new RoleAuditLog();
        auditLog.setUser(user);
        auditLog.setOldRole(user.getRole());
        auditLog.setNewRole(newRole);
        auditLog.setChangedAt(LocalDateTime.now());

        // Обработка для TEACHER
        if(user.getRole().equals("ROLE_TEACHER")){
            log.debug("Пользователь id={} был TEACHER, отвязываем от групп", userId);
            List<Group> groups = groupRepository.findByTeacherId(userId);
            for(Group group : groups) {
                group.setTeacher(null);
                groupRepository.save(group);
                log.debug("Преподаватель id={} отвязан от группы id={}", userId, group.getId());
            }
        }

        if(user.getRole().equals("ROLE_STUDENT")){
            log.debug("Пользователь id={} был STUDENT, удаляем из группы", userId);
            Integer groupId = groupServiceImpl.getUserGroup(user.getUsername());
            if (groupId != null) {
                groupServiceImpl.removeStudentFromGroup(groupId, userId);
                log.debug("Студент id={} удален из группы id={}", userId, groupId);
            }
        }

        user.setRole(newRole);
        peopleService.save(user);
        log.info("Роль пользователя id={} изменена с {} на {}", userId, auditLog.getOldRole(), newRole);

        //уведомление: вам назначили новую роль
        notificationProducerServiceImpl.sendChangeRoleNotification(
                userId,
                newRole
        );
        log.debug("Уведомление о смене роли отправлено пользователю id={}", userId);

        roleAuditLogRepository.save(auditLog);
        log.debug("Аудит смены роли сохранен: id={}", auditLog.getId());
    }

    @Transactional
    @Override
    public void assignTeacherToGroup(Integer groupId, Integer userId) {
        log.info("Назначение преподавателя id={} группе id={}", userId, groupId);

        Group group = groupRepository.findById(groupId).orElseThrow(() -> {
            log.error("Группа id={} не найдена при назначении преподавателя", groupId);
            return new EntityNotFoundException("Group not found with id");
        });
        Person user = peopleService.findById(userId).orElseThrow(() -> {
            log.error("Пользователь id={} не найден при назначении преподавателем", userId);
            return new EntityNotFoundException("User not found");
        });
        Person previousTeacher = group.getTeacher();

        group.setTeacher(user);
        groupRepository.save(group);
        log.info("Преподаватель id={} назначен группе id={}", userId, groupId);

        if (previousTeacher != null && !previousTeacher.getId().equals(userId)) {
            notificationProducerServiceImpl.sendTeacherRemovedNotification(
                    previousTeacher.getId(),
                    group.getName()
            );
            log.debug("Уведомление об удалении отправлено предыдущему преподавателю id={}", previousTeacher.getId());
        }

        notificationProducerServiceImpl.sendTeacherAssignNotification(
                user.getId(),
                group.getName()
        );
        log.debug("Уведомление о назначении отправлено преподавателю id={}", user.getId());
    }

    @Transactional
    @Override
    public Person createTeacher(Integer id) {
        log.info("Назначение пользователя id={} преподавателем", id);

        Person teacher = peopleService.findById(id).orElseThrow(
                () -> {
                    log.error("Пользователь id={} не найден при назначении преподавателем", id);
                    return new EntityNotFoundException("User not found");
                });

        RoleAuditLog auditLog = new RoleAuditLog();
        auditLog.setUser(teacher);
        auditLog.setOldRole(teacher.getRole());
        auditLog.setNewRole("ROLE_TEACHER");
        auditLog.setChangedAt(LocalDateTime.now());

        teacher.setRole("ROLE_TEACHER");
        teacher.setCreatedAt(LocalDateTime.now());

        Person savedTeacher = peopleService.save(teacher);
        log.info("Пользователь id={} успешно назначен преподавателем", id);

        notificationProducerServiceImpl.sendChangeRoleNotification(
                id,
                "TEACHER"
        );

        roleAuditLogRepository.save(auditLog);
        log.debug("Аудит назначения преподавателя сохранен: id={}", auditLog.getId());

        return savedTeacher;
    }

    @Override
    public Page<Person> getUsersByRole(String role, Pageable page) {
        log.debug("Получение пользователей с ролью: {}, page={}, size={}",
                role, page.getPageNumber(), page.getPageSize());
        return peopleService.findByRole("ROLE_" + role, page);
    }

    @Override
    public List<LogResponseDTO> getRoleAuditLogs() {
        log.debug("Получение всех логов аудита ролей");
        List<RoleAuditLog> logs = roleAuditLogRepository.findAll();
        log.debug("Получено {} записей аудита ролей", logs.size());
        return convertToLogResponse(logs);
    }

    @Override
    public DashboardStatsDTO getDashboardStats(Authentication auth) {
        String username = auth.getName();
        String role = auth.getAuthorities().iterator().next().getAuthority();
        log.info("Формирование статистики дашборда для пользователя: {}, роль: {}", username, role);

        PersonResponseDTO userInfo = peopleService.getUserInfo(username);

        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setUserInfo(userInfo);

        switch (role) {
            case "ROLE_ADMIN":
                log.debug("Формирование статистики для ADMIN");
                // Общее количество пользователей
                stats.setTotalUsers(peopleService.findAll().size());

                // Общее количество групп
                stats.setTotalGroups(groupRepository.findAll().size());

                // Общее количество задач (всех задач в системе)
                stats.setTotalTasks(taskServiceImpl.findAllTasks().size());

                // Статистика по ролям
                Map<String, Integer> roleStats = new HashMap<>();
                for (Person person : peopleService.findAll()) {
                    String userRole = person.getRole();
                    roleStats.put(userRole, roleStats.getOrDefault(userRole, 0) + 1);
                }
                stats.setRoleStatistics(roleStats);
                log.debug("Статистика для ADMIN сформирована: users={}, groups={}, tasks={}",
                        stats.getTotalUsers(), stats.getTotalGroups(), stats.getTotalTasks());
                break;

            case "ROLE_TEACHER":
                log.debug("Формирование статистики для TEACHER");
                Person teacher = peopleService.findByUsername(username)
                        .orElseThrow(() -> {
                            log.error("Преподаватель {} не найден", username);
                            return new EntityNotFoundException("Teacher not found");
                        });

                // Группы преподавателя
                List<Group> teacherGroups = groupRepository.findByTeacherId(teacher.getId());
                stats.setTotalGroups(teacherGroups.size());

                // Студенты преподавателя (все студенты из его групп)
                List<Person> teacherStudents = groupServiceImpl.findByTeacherId(auth);
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

                // 3. Просрочки
                int totalOverdueTasks = 0;
                for (Person student : teacherStudents) {
                    List<TaskAssignment> studentAssignments = taskAssignmentRepository.findByUserId(student.getId());
                    long studentOverdueCount = studentAssignments.stream()
                            .filter(assignment -> {
                                Task task = assignment.getTask();
                                return task != null &&
                                        task.getAuthor() != null &&
                                        task.getAuthor().getId().equals(teacher.getId()) &&
                                        task.getDeadline() != null &&
                                        task.getDeadline().isBefore(LocalDateTime.now()) &&
                                        !"COMPLETED".equals(assignment.getStatus());
                            })
                            .count();
                    totalOverdueTasks += (int) studentOverdueCount;
                }
                stats.setTotalOverdueTasks(totalOverdueTasks);

                // 4. Стагнация
                LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);
                List<TaskAssignment> stuckAssignments = taskAssignmentRepository.findStuckByTeacherId(teacher.getId(), twoWeeksAgo);
                stats.setStuckTasks(stuckAssignments.size());

                // 5. Задачи в группах препода
                long tasksInTeacherGroups = teacherTasks.stream()
                        .filter(task -> !taskAssignmentRepository.findByTaskId(task.getId()).isEmpty())
                        .count();
                stats.setTasksAssignedToMyGroups((int) tasksInTeacherGroups);
                long totalSystemTasks = taskRepository.count();
                stats.setTotalTasks((int) totalSystemTasks);

                log.debug("Статистика для TEACHER сформирована: students={}, groups={}, tasks={}",
                        stats.getTotalStudents(), stats.getTotalGroups(), stats.getMyCreatedTasks());
                break;

            case "ROLE_STUDENT":
                log.debug("Формирование статистики для STUDENT");
                Person student = peopleService.findByUsername(username)
                        .orElseThrow(() -> {
                            log.error("Студент {} не найден", username);
                            return new EntityNotFoundException("Student not found");
                        });

                List<TaskAssignment> studentAssignments = taskAssignmentRepository.findByUserId(student.getId());

                int activeStudentTasks = 0;
                int completedStudentTasks = 0;
                Map<String, Integer> statusCount = new HashMap<>();
                LocalDateTime nextDeadline = null;

                for (TaskAssignment assignment : studentAssignments) {
                    String status = assignment.getStatus();

                    statusCount.put(status, statusCount.getOrDefault(status, 0) + 1);

                    if ("COMPLETED".equals(status)) {
                        completedStudentTasks++;
                    }
                    else {
                        activeStudentTasks++;

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

                log.debug("Статистика для STUDENT сформирована: active={}, completed={}, total={}",
                        stats.getActiveTasks(), stats.getCompletedTasks(), studentAssignments.size());
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
