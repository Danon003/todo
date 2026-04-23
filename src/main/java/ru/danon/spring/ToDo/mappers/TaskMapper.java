package ru.danon.spring.ToDo.mappers;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.danon.spring.ToDo.dto.MyTaskDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.dto.TagDTO;
import ru.danon.spring.ToDo.dto.TaskDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.models.postgre.Tag;
import ru.danon.spring.ToDo.models.postgre.Task;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskMapper {

    private final ModelMapper modelMapper;

    /**
     * Task -> TaskResponseDTO
     */
    public TaskResponseDTO toResponseDto(Task task) {
        if (task == null) return null;

        // Явно создаём DTO и заполняем поля
        TaskResponseDTO dto = new TaskResponseDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setDeadline(task.getDeadline());
        dto.setPriority(task.getPriority());
        dto.setCreatedAt(task.getCreatedAt());

        // Author
        if (task.getAuthor() != null) {
            TaskResponseDTO.AuthorDTO authorDto = new TaskResponseDTO.AuthorDTO();
            authorDto.setUsername(task.getAuthor().getUsername());
            authorDto.setEmail(task.getAuthor().getEmail());
            authorDto.setRole(task.getAuthor().getRole());
            dto.setAuthor(authorDto);
        }

        dto.setTags(mapTags(task.getTaskTags()));

        return dto;
    }

    public TaskResponseDTO convertToResponseDto(Task task) {
        return modelMapper.map(task, TaskResponseDTO.class);
    }

    /**
     * Task -> TaskDTO
     */
    public TaskDTO toDto(Task task) {
        if (task == null) return null;

        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setDeadline(task.getDeadline());
        dto.setPriority(task.getPriority());

        if (task.getAuthor() != null) {
            dto.setAuthorId(task.getAuthor().getId());
        }

        return dto;
    }

    /**
     * Task + мапа тегов -> TaskDTO
     */
    public TaskDTO toDto(Task task, Map<Integer, List<Tag>> tagsByTask) {
        TaskDTO dto = toDto(task);
        if (dto != null && tagsByTask != null) {
            List<Tag> tags = tagsByTask.getOrDefault(task.getId(), Collections.emptyList());
            dto.setTags(mapTagsFromTagList(tags));
        }
        return dto;
    }

    public PersonResponseDTO convertToPersonDTO(Person user) {
        return modelMapper.map(user, PersonResponseDTO.class);
    }

    /**
     * Page<Task> + теги -> Page<TaskDTO>
     */
    public Page<TaskDTO> toDtoPage(Page<Task> page, Map<Integer, List<Tag>> tagsByTask) {
        if (page == null) return Page.empty();
        return page.map(task -> toDto(task, tagsByTask));
    }

    /**
     * Маппинг тегов из TaskTag связи
     */
    private List<TagDTO> mapTags(List<?> taskTags) {
        if (taskTags == null || taskTags.isEmpty()) {
            return Collections.emptyList();
        }
        return taskTags.stream()
                .filter(Objects::nonNull)
                .map(tt -> {
                    // Получаем Tag через рефлексию (безопаснее)
                    try {
                        java.lang.reflect.Method getTag = tt.getClass().getMethod("getTag");
                        Tag tag = (Tag) getTag.invoke(tt);
                        return tag != null ? new TagDTO(tag.getId(), tag.getName()) : null;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Маппинг тегов из списка Tag
     */
    private List<TagDTO> mapTagsFromTagList(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(tag -> new TagDTO(tag.getId(), tag.getName()))
                .collect(Collectors.toList());
    }
}