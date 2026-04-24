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
    void addTagToTask(Long taskId, Long tagId);

    @Transactional
    void addTagToTaskByName(Long taskId, String name);

    @Transactional
    void removeTagFromTask(Long taskId, Long tagId);

    List<Tag> getTaskTags(Long taskId);

    Map<Long, List<Tag>> getTaskTagsBatch(Collection<Long> taskIds);

    List<Task> getTaskByTag(String tagName);
}
