package ru.danon.spring.ToDo.repositories.jpa;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.models.postgre.TaskTag;
import ru.danon.spring.ToDo.models.postgre.id.TaskTagId;

import java.util.Collection;
import java.util.List;

@Repository
public interface TaskTagRepository extends JpaRepository<TaskTag, TaskTagId> {
    List<TaskTag> findByTaskId(Long taskId);
    List<TaskTag> findByTagId(Long tagId);
    void deleteByTaskId(Long taskId);
    @Query("SELECT tt FROM TaskTag tt JOIN FETCH tt.tag WHERE tt.taskId = :taskId")
    List<TaskTag> findTaskTagsWithTagsByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT tt FROM TaskTag tt JOIN FETCH tt.tag WHERE tt.taskId IN :taskIds")
    List<TaskTag> findTaskTagsWithTagsByTaskIds(@Param("taskIds") Collection<Long> taskIds);
    boolean existsByTaskIdAndTagId(Long taskId, Long tagId);

    List<Task> findTasksByTag_Name(@NotNull(message = "Имя тега не должно быть пустым")
                                   @UniqueElements
                                   @Size(min = 2, max = 50, message = "Название тега должно быть от 2 до 50 символов") String tagName);

    void deleteByTaskIdAndTagId(Long taskId, Long tagId);
}