package ru.danon.spring.ToDo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.models.Tag;
import ru.danon.spring.ToDo.models.Task;
import ru.danon.spring.ToDo.models.TaskTag;
import ru.danon.spring.ToDo.repositories.TagRepository;
import ru.danon.spring.ToDo.repositories.TaskRepository;
import ru.danon.spring.ToDo.repositories.TaskTagRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagService {
    private final TagRepository tagRepository;
    private final TaskRepository taskRepository;
    private final TaskTagRepository taskTagRepository;

    @Autowired
    public TagService(TagRepository tagRepository, TaskRepository taskRepository, TaskTagRepository taskTagRepository) {
        this.tagRepository = tagRepository;
        this.taskRepository = taskRepository;
        this.taskTagRepository = taskTagRepository;
    }

    public List<Tag> getAllTags(){
        return tagRepository.findAll();
    }

    @Transactional
    public void createTag(Tag tag) {
        if (tagRepository.findByName(tag.getName()).isPresent()) {
            throw new RuntimeException("Тег уже существует: " + tag);
        }

        Tag newTag = new Tag();
        newTag.setName(tag.getName());
        tagRepository.save(newTag);
    }

    @Transactional
    public void addTagToTask(Integer taskId, Integer tagId) {
        if (taskTagRepository.existsByTaskIdAndTagId(taskId, tagId)) {
            return;
        }
        TaskTag taskTag = new TaskTag();
        taskTag.setTaskId(taskId);
        taskTag.setTagId(tagId);

        taskTagRepository.save(taskTag);
    }

    @Transactional
    public void addTagToTaskByName(Integer taskId, String name) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Задача с ID " + taskId + " не найдена"));

        // Проверяем, существует ли тег
        Tag tag = tagRepository.findByName(name)
                .orElseGet(() -> {
                    Tag newTag = new Tag();
                    newTag.setName(name);
                    return tagRepository.save(newTag);
                });

        // Проверяем, не добавлен ли уже этот тег к задаче
        if (taskTagRepository.existsByTaskIdAndTagId(taskId, tag.getId())) {
            return;
        }

        TaskTag taskTag = new TaskTag();
        taskTag.setTaskId(taskId);
        taskTag.setTagId(tag.getId());

        taskTagRepository.save(taskTag);
    }

    @Transactional
    public void removeTagFromTask(Integer taskId, Integer tagId) {
        taskTagRepository.deleteByTaskIdAndTagId(tagId, taskId);
    }

    public List<Tag> getTaskTags(Integer taskId) {
        try {
            return taskTagRepository.findByTaskId(taskId)
                    .stream()
                    .map(TaskTag::getTag)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<Task> getTaskByTag(String tagName) {
        return taskTagRepository.findTasksByTag_Name(tagName);
    }

}
