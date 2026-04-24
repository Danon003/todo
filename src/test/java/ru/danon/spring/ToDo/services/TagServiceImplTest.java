package ru.danon.spring.ToDo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.models.postgre.Tag;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.models.postgre.TaskTag;
import ru.danon.spring.ToDo.repositories.jpa.TagRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskTagRepository;
import ru.danon.spring.ToDo.services.impl.TagServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagRepository tagRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskTagRepository taskTagRepository;

    @InjectMocks
    private TagServiceImpl tagService;

    @Test
    void createTagShouldThrowWhenTagAlreadyExists() {
        Tag tag = new Tag();
        tag.setName("java");
        when(tagRepository.findByName("java")).thenReturn(Optional.of(new Tag()));

        assertThrows(IllegalArgumentException.class, () -> tagService.createTag(tag));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void createTagShouldSaveNewTag() {
        Tag tag = new Tag();
        tag.setName("backend");
        when(tagRepository.findByName("backend")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        tagService.createTag(tag);

        ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(captor.capture());
        assertEquals("backend", captor.getValue().getName());
    }

    @Test
    void addTagToTaskShouldSkipWhenAlreadyExists() {
        when(taskTagRepository.existsByTaskIdAndTagId(1L, 2L)).thenReturn(true);

        tagService.addTagToTask(1L, 2L);

        verify(taskTagRepository, never()).save(any());
    }

    @Test
    void addTagToTaskByNameShouldThrowWhenTaskMissing() {
        when(taskRepository.findById(77L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> tagService.addTagToTaskByName(77L, "java"));
    }

    @Test
    void addTagToTaskByNameShouldCreateAndLinkNewTag() {
        Task task = new Task();
        task.setId(1L);
        Tag savedTag = new Tag();
        savedTag.setId(10L);
        savedTag.setName("spring");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(tagRepository.findByName("spring")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);
        when(taskTagRepository.existsByTaskIdAndTagId(1L, 10L)).thenReturn(false);

        tagService.addTagToTaskByName(1L, "spring");

        ArgumentCaptor<TaskTag> captor = ArgumentCaptor.forClass(TaskTag.class);
        verify(taskTagRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getTaskId());
        assertEquals(10L, captor.getValue().getTagId());
    }

    @Test
    void getTaskTagsBatchShouldReturnGroupedTags() {
        Tag tag = new Tag();
        tag.setId(3L);
        tag.setName("sql");
        TaskTag taskTag = new TaskTag();
        taskTag.setTaskId(5L);
        taskTag.setTag(tag);
        when(taskTagRepository.findTaskTagsWithTagsByTaskIds(Set.of(5L)))
                .thenReturn(List.of(taskTag));

        Map<Long, List<Tag>> result = tagService.getTaskTagsBatch(Set.of(5L));

        assertEquals(1, result.size());
        assertEquals("sql", result.get(5L).getFirst().getName());
    }

    @Test
    void getTaskTagsShouldReturnEmptyListOnRepositoryException() {
        when(taskTagRepository.findTaskTagsWithTagsByTaskId(1L)).thenThrow(new RuntimeException("db error"));

        List<Tag> result = tagService.getTaskTags(1L);

        assertTrue(result.isEmpty());
    }
}
