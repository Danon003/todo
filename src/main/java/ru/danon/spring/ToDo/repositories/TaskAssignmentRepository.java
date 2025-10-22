package ru.danon.spring.ToDo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.models.Task;
import ru.danon.spring.ToDo.models.TaskAssignment;
import ru.danon.spring.ToDo.models.id.TaskAssignmentId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, TaskAssignmentId> {
    List<TaskAssignment> findByTask(Task task);
    List<TaskAssignment> findByUser(Person user);

    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.user.id = :userId AND ta.task.id = :taskId")
    Optional<TaskAssignment> findByUserIdAndTaskId(@Param("userId") Integer userId,
                                                   @Param("taskId") Integer taskId);

    @Query("SELECT ta FROM TaskAssignment ta JOIN ta.user u JOIN UserGroup ug ON u.id = ug.user.id " +
            "WHERE ug.group.id = :groupId AND ta.task.id = :taskId")
    List<TaskAssignment> findByGroupIdAndTaskId(@Param("groupId") Integer groupId,
                                                @Param("taskId") Integer taskId);

    void deleteByTaskId(Integer taskId);

    boolean existsByTaskAndUser(Task task, Person user);

    @Modifying
    @Query("UPDATE TaskAssignment ta SET ta.status = 'OVERDUE' " +
            "WHERE ta.task.deadline < :now " +
            "AND ta.status IN ('NOT_STARTED', 'IN_PROGRESS')")
    void updateOverdueTaskAssignments(@Param("now") LocalDateTime now);

    @Query("SELECT ta FROM TaskAssignment ta " +
            "WHERE ta.task.deadline < :now " +
            "AND ta.status IN ('NOT_STARTED', 'IN_PROGRESS')")
    List<TaskAssignment> findOverdueTaskAssignments(@Param("now") LocalDateTime now);

    @Query("SELECT ta FROM TaskAssignment ta " +
            "WHERE ta.task.deadline BETWEEN :windowStart AND :windowEnd " +
            "AND ta.status IN ('NOT_STARTED', 'IN_PROGRESS')")
    List<TaskAssignment> findTasksWithDeadlineInWindow(
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd
    );
    List<TaskAssignment> findByTaskId(Integer taskId);
    List<TaskAssignment> findByUserId(Integer userId);

    @Query("SELECT ta FROM TaskAssignment ta JOIN ta.task t WHERE t.author.id = :teacherId AND ta.status = 'IN_PROGRESS' AND ta.updated_At < :twoWeeksAgo")
    List<TaskAssignment> findStuckByTeacherId(@Param("teacherId") Integer teacherId, @Param("twoWeeksAgo") LocalDateTime twoWeeksAgo);

}
