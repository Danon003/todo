package ru.danon.spring.ToDo.repositories.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.models.postgre.TaskAssignment;
import ru.danon.spring.ToDo.models.postgre.id.TaskAssignmentId;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, TaskAssignmentId> {
    List<TaskAssignment> findByTask(Task task);

    @EntityGraph(attributePaths = {"task", "task.author", "task.taskTags", "task.taskTags.tag", "user"})
    List<TaskAssignment> findByUser(Person user);

    @EntityGraph(attributePaths = {"task", "task.author", "task.taskTags", "task.taskTags.tag", "user"})
    Page<TaskAssignment> findByUser(Person user, Pageable pageable);

    @EntityGraph(attributePaths = {"task", "task.author", "task.taskTags", "task.taskTags.tag", "user"})
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.user = :user AND ta.status <> 'OVERDUE'")
    Page<TaskAssignment> findActiveByUser(@Param("user") Person user, Pageable pageable);

    @EntityGraph(attributePaths = {"task", "task.author", "task.taskTags", "task.taskTags.tag", "user"})
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.user.id = :userId AND ta.task.id = :taskId")
    Optional<TaskAssignment> findByUserIdAndTaskId(@Param("userId") Long userId,
                                                   @Param("taskId") Long taskId);

    @Query("SELECT ta FROM TaskAssignment ta JOIN ta.user u JOIN UserGroup ug ON u.id = ug.user.id " +
            "WHERE ug.group.id = :groupId AND ta.task.id = :taskId")
    List<TaskAssignment> findByGroupIdAndTaskId(@Param("groupId") Long groupId,
                                                @Param("taskId") Long taskId);

    void deleteByTaskId(Long taskId);

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
            @Param("windowEnd") LocalDateTime windowEnd,
            @Param("now") LocalDateTime now
    );
    @EntityGraph(attributePaths = {"user"})
    List<TaskAssignment> findByTaskId(Long taskId);

    @EntityGraph(attributePaths = {"task", "task.author", "task.taskTags", "task.taskTags.tag", "user"})
    List<TaskAssignment> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"task", "task.author", "task.taskTags", "task.taskTags.tag", "user"})
    List<TaskAssignment> findByUserIdIn(Collection<Long> userIds);

    @EntityGraph(attributePaths = {"task", "task.author", "task.taskTags", "task.taskTags.tag", "user"})
    List<TaskAssignment> findByUserAndStatusNot(Person user, String status, Sort sort);

    @EntityGraph(attributePaths = {"task", "task.author", "task.taskTags", "task.taskTags.tag", "user"})
    List<TaskAssignment> findByUserAndStatus(Person user, String status, Sort sort);

    @Modifying
    @Query("DELETE FROM TaskAssignment ta WHERE ta.user.id = :userId AND ta.status = 'OVERDUE'")
    int deleteOverdueAssignmentsByUserId(@Param("userId") Long userId);

    @Query("SELECT ta FROM TaskAssignment ta JOIN ta.task t WHERE ta.assignedBy.id = :teacherId AND ta.status != 'COMPLETED' AND ta.status != 'OVERDUE' AND ta.updated_At < :twoWeeksAgo")
    List<TaskAssignment> findStuckByTeacherId(@Param("teacherId") Long teacherId, @Param("twoWeeksAgo") LocalDateTime twoWeeksAgo);

    @Query("SELECT COUNT(ta) > 0 FROM TaskAssignment ta " +
            "JOIN ta.task t " +
            "WHERE t.id = :taskId " +
            "AND ta.userId = :userId " +
            "AND ta.status NOT IN ('COMPLETED', 'OVERDUE') " +
            "AND t.deadline > :now")
    boolean existsValidTaskForNotification(@Param("taskId") Long taskId,
                                           @Param("userId") Long userId,
                                           @Param("now") LocalDateTime now);}
