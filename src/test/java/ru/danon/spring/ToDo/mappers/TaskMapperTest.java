package ru.danon.spring.ToDo.mappers;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.danon.spring.ToDo.dto.TaskDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.models.postgre.Tag;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.models.postgre.TaskTag;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaskMapperTest {

    private final TaskMapper taskMapper = new TaskMapper(new ModelMapper());

    @Test
    void toResponseDtoShouldReturnNullForNullTask() {
        assertNull(taskMapper.toResponseDto(null));
    }

    @Test
    void toResponseDtoShouldMapAuthorAndTags() {
        Person author = new Person();
        author.setUsername("teacher");
        author.setEmail("t@mail.com");
        author.setRole("ROLE_TEACHER");

        Tag tag = new Tag();
        tag.setId(3L);
        tag.setName("java");
        TaskTag taskTag = new TaskTag();
        taskTag.setTag(tag);

        Task task = new Task();
        task.setId(1L);
        task.setTitle("Task");
        task.setAuthor(author);
        task.setTaskTags(List.of(taskTag));

        TaskResponseDTO dto = taskMapper.toResponseDto(task);

        assertEquals(1L, dto.getId());
        assertEquals("teacher", dto.getAuthor().getUsername());
        assertEquals(1, dto.getTags().size());
        assertEquals("java", dto.getTags().getFirst().getName());
    }

    @Test
    void toDtoShouldMapBasicFieldsAndAuthorId() {
        Person author = new Person();
        author.setId(9L);
        Task task = new Task();
        task.setId(2L);
        task.setTitle("T");
        task.setAuthor(author);

        TaskDTO dto = taskMapper.toDto(task);

        assertEquals(2L, dto.getId());
        assertEquals(9L, dto.getAuthorId());
    }

    @Test
    void toDtoWithTagsShouldAddTagsFromBatchMap() {
        Task task = new Task();
        task.setId(4L);
        Tag tag = new Tag();
        tag.setId(7L);
        tag.setName("spring");

        TaskDTO dto = taskMapper.toDto(task, Map.of(4L, List.of(tag)));

        assertEquals(1, dto.getTags().size());
        assertEquals("spring", dto.getTags().getFirst().getName());
    }

    @Test
    void toDtoPageShouldMapPageContent() {
        Task task = new Task();
        task.setId(11L);
        var page = new PageImpl<>(List.of(task), PageRequest.of(0, 5), 1);

        var dtoPage = taskMapper.toDtoPage(page, Map.of());

        assertEquals(1, dtoPage.getTotalElements());
        assertEquals(11L, dtoPage.getContent().getFirst().getId());
    }
}
