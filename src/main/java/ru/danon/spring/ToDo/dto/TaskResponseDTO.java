package ru.danon.spring.ToDo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Data
public class TaskResponseDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private String priority;
    private List<TagDTO> tags;
    private AuthorDTO author;
    private LocalDateTime createdAt;

    @Data
    public static class AuthorDTO {
        private String username;
        private String email;
        private String role;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskResponseDTO that = (TaskResponseDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
