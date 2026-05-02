package ru.danon.spring.ToDo.repositories.jpa;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.postgre.KanbanTask;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface KanbanTaskRepository extends JpaRepository<KanbanTask, Long> {

    @EntityGraph(attributePaths = {"task", "meeting", "user"})
    List<KanbanTask> findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscPositionAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    @EntityGraph(attributePaths = {"task", "meeting", "user"})
    Optional<KanbanTask> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"task", "meeting", "user"})
    List<KanbanTask> findByUserIdAndScheduledDateOrderByPositionAsc(Long userId, LocalDate scheduledDate);

    Optional<KanbanTask> findByUserIdAndTaskId(Long userId, Long taskId);

    Optional<KanbanTask> findByUserIdAndMeetingId(Long userId, Long meetingId);

    @Modifying
    @Query("DELETE FROM KanbanTask kt WHERE kt.user.id = :userId AND kt.task.id = :taskId")
    void deleteByUserIdAndTaskId(Long userId, Long taskId);
}
