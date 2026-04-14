package ru.danon.spring.ToDo.services;

import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.models.postgre.Tag;
import ru.danon.spring.ToDo.models.postgre.Task;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TagService {
    List<Tag> getAllTags();

    @Transactional
    void createTag(Tag tag);

    @Transactional
    void addTagToTask(Integer taskId, Integer tagId);

    @Transactional
    void addTagToTaskByName(Integer taskId, String name);

    @Transactional
    void removeTagFromTask(Integer taskId, Integer tagId);

    List<Tag> getTaskTags(Integer taskId);

    Map<Integer, List<Tag>> getTaskTagsBatch(Collection<Integer> taskIds);

    List<Task> getTaskByTag(String tagName);
}
