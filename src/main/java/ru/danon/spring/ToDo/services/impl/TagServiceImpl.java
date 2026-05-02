package ru.danon.spring.ToDo.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.models.postgre.Tag;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.models.postgre.TaskTag;
import ru.danon.spring.ToDo.repositories.jpa.TagRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskTagRepository;
import ru.danon.spring.ToDo.services.TagService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class TagServiceImpl implements TagService {
    private final TagRepository tagRepository;
    private final TaskRepository taskRepository;
    private final TaskTagRepository taskTagRepository;

    @Override
    public List<Tag> getAllTags(){
        log.debug("Получение всех тегов");
        List<Tag> tags = tagRepository.findAll();
        log.debug("Получено {} тегов", tags.size());
        return tags;
    }

    @Transactional
    @Override
    public void createTag(Tag tag) {
        log.info("Создание тега: {}", tag.getName());

        if (tagRepository.findByName(tag.getName()).isPresent()) {
            log.warn("Попытка создания существующего тега: {}", tag.getName());
            throw new IllegalArgumentException("Тег уже существует: " + tag);
        }

        Tag newTag = new Tag();
        newTag.setName(tag.getName());
        Tag savedTag = tagRepository.save(newTag);
        log.info("Тег успешно создан: id={}, name={}", savedTag.getId(), savedTag.getName());
    }

    @Transactional
    @Override
    public void addTagToTask(Long taskId, Long tagId) {
        if (taskTagRepository.existsByTaskIdAndTagId(taskId, tagId)) {
            log.debug("Тег id={} уже привязан к задаче id={}", tagId, taskId);
            return;
        }

        log.debug("Добавление тега id={} к задаче id={}", tagId, taskId);
        TaskTag taskTag = new TaskTag();
        taskTag.setTaskId(taskId);
        taskTag.setTagId(tagId);

        taskTagRepository.save(taskTag);
        log.debug("Тег id={} успешно добавлен к задаче id={}", tagId, taskId);
    }

    @Transactional
    @Override
    public void addTagToTaskByName(Long taskId, String name) {
        log.debug("Добавление тега '{}' к задаче id={}", name, taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.error("Задача id={} не найдена", taskId);
                    return new EntityNotFoundException("Задача с ID " + taskId + " не найдена");
                });

        // Проверяем, существует ли тег
        Tag tag = tagRepository.findByName(name)
                .orElseGet(() -> {
                    log.debug("Тег '{}' не существует, создаем новый", name);
                    Tag newTag = new Tag();
                    newTag.setName(name);
                    return tagRepository.save(newTag);
                });

        // Проверяем, не добавлен ли уже этот тег к задаче
        if (taskTagRepository.existsByTaskIdAndTagId(taskId, tag.getId())) {
            log.debug("Тег '{}' уже привязан к задаче id={}", name, taskId);
            return;
        }

        TaskTag taskTag = new TaskTag();
        taskTag.setTaskId(taskId);
        taskTag.setTagId(tag.getId());

        taskTagRepository.save(taskTag);
        log.debug("Тег '{}' успешно добавлен к задаче id={}", name, taskId);
    }

    @Transactional
    @Override
    public void removeTagFromTask(Long taskId, Long tagId) {
        log.debug("Удаление тега id={} из задачи id={}", tagId, taskId);
        taskTagRepository.deleteByTaskIdAndTagId(tagId, taskId);
        log.debug("Тег id={} успешно удален из задачи id={}", tagId, taskId);
    }

    @Override
    public List<Tag> getTaskTags(Long taskId) {
        log.debug("Получение тегов задачи id={}", taskId);
        try {
            List<Tag> tags = taskTagRepository.findTaskTagsWithTagsByTaskId(taskId)
                    .stream()
                    .map(TaskTag::getTag)
                    .collect(Collectors.toList());
            log.debug("Получено {} тегов для задачи id={}", tags.size(), taskId);
            return tags;
        } catch (Exception e) {
            log.error("Ошибка при получении тегов задачи id={}", taskId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<Long, List<Tag>> getTaskTagsBatch(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("Пакетное получение тегов для {} задач", taskIds.size());
        Map<Long, List<Tag>> result = taskTagRepository.findTaskTagsWithTagsByTaskIds(taskIds).stream()
                .filter(taskTag -> taskTag.getTaskId() != null && taskTag.getTag() != null)
                .collect(Collectors.groupingBy(
                        TaskTag::getTaskId,
                        Collectors.mapping(TaskTag::getTag, Collectors.toList())
                ));
        log.debug("Теги получены для {} задач", result.size());
        return result;
    }

    @Override
    public List<Task> getTaskByTag(String tagName) {
        log.debug("Поиск задач по тегу: {}", tagName);
        List<Task> tasks = taskTagRepository.findTasksByTag_Name(tagName);
        log.debug("Найдено {} задач с тегом '{}'", tasks.size(), tagName);
        return tasks;
    }
}
