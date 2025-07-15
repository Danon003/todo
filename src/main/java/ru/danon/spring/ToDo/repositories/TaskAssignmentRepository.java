package ru.danon.spring.ToDo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.models.Task;
import ru.danon.spring.ToDo.models.TaskAssignment;
import ru.danon.spring.ToDo.models.id.TaskAssignmentId;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, TaskAssignmentId> {
    List<TaskAssignment> findByTask(Task task);
    List<TaskAssignment> findByUser(Person user);
    void deleteByTask(Task task);

    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.user.id = :userId AND ta.task.id = :taskId")
    Optional<TaskAssignment> findByUserIdAndTaskId(@Param("userId") Integer userId,
                                                   @Param("taskId") Integer taskId);

    @Query("SELECT ta FROM TaskAssignment ta JOIN ta.user u JOIN UserGroup ug ON u.id = ug.user.id " +
            "WHERE ug.group.id = :groupId AND ta.task.id = :taskId")
    List<TaskAssignment> findByGroupIdAndTaskId(@Param("groupId") Integer groupId,
                                                @Param("taskId") Integer taskId);

    void deleteByTaskId(Integer taskId);

    boolean existsByTaskAndUser(Task task, Person user);
}
