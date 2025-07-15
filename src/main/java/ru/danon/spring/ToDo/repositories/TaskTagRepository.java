package ru.danon.spring.ToDo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.TaskTag;
import ru.danon.spring.ToDo.models.id.TaskTagId;

import java.util.List;

@Repository
public interface TaskTagRepository extends JpaRepository<TaskTag, TaskTagId> {
    List<TaskTag> findByTaskId(Integer taskId);
    List<TaskTag> findByTagId(Integer tagId);
    void deleteByTaskId(Integer taskId);
}