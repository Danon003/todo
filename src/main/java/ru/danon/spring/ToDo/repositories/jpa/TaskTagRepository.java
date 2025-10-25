package ru.danon.spring.ToDo.repositories.jpa;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.Task;
import ru.danon.spring.ToDo.models.TaskTag;
import ru.danon.spring.ToDo.models.id.TaskTagId;

import java.util.List;

@Repository
public interface TaskTagRepository extends JpaRepository<TaskTag, TaskTagId> {
    List<TaskTag> findByTaskId(Integer taskId);
    List<TaskTag> findByTagId(Integer tagId);
    void deleteByTaskId(Integer taskId);
    @Query("SELECT tt FROM TaskTag tt JOIN FETCH tt.tag WHERE tt.task.id = :taskId")
    List<TaskTag> findTaskTagsWithTagsByTaskId(@Param("taskId") Integer taskId);
    boolean existsByTaskIdAndTagId(Integer taskId, Integer tagId);

    List<Task> findTasksByTag_Name(@NotNull(message = "Имя тега не должно быть пустым") @UniqueElements @Size(min = 2, max = 50, message = "Название тега должно быть от 2 до 50 символов") String tagName);

    void deleteByTaskIdAndTagId(Integer taskId, Integer tagId);
}