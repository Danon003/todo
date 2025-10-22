package ru.danon.spring.ToDo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.TaskFile;

import java.util.List;

@Repository
public interface TaskFileRepository extends JpaRepository<TaskFile, Integer> {
    List<TaskFile> findByTaskId(Integer taskId);
    void deleteByTaskId(Integer taskId);
}