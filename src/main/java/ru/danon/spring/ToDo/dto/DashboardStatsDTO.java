package ru.danon.spring.ToDo.dto;

import lombok.Data;
import ru.danon.spring.ToDo.models.Group;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStatsDTO {
    private PersonResponseDTO userInfo;
    private Integer totalUsers;
    private Integer totalGroups;
    private Integer totalTasks;
    private Integer activeTasks;
    private Integer completedTasks;
    private Integer totalStudents;
    private Integer totalAssignedTasks;
    private Map<String, Integer> roleStatistics;
    private Map<String, Integer> statusCount;
    private List<Group> myGroups;
    private LocalDateTime nextDeadline;
    private List<ActivityDTO> recentActivity;

    // Новые поля для дашборда препода
    private Double avgStudentProgress;          // Средний прогресс студентов %
    private Double avgTasksPerStudent;          // Средняя нагрузка
    private Integer minTasks;                   // Минимальная нагрузка
    private Integer maxTasks;                   // Максимальная нагрузка
    private Integer totalOverdueTasks;          // Всего просроченных задач
    private Integer stuckTasks;                 // Задач в стагнации
    private Integer myCreatedTasks;             // Задач создано преподом
    private Integer tasksAssignedToMyGroups;    // Задач назначено в группы препода
}

