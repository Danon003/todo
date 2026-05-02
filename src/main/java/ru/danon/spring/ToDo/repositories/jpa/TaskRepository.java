package ru.danon.spring.ToDo.repositories.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.postgre.Task;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAuthorId(Long authorId);

    Task findTaskById(Long taskId);

    @Query("SELECT t from Task t where t.deadline > CURRENT_TIMESTAMP")
    Page<Task> findActiveTasks(Pageable pageable);
}
