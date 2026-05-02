package ru.danon.spring.ToDo.models.postgre.id;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class TaskTagId implements Serializable {
    private Long taskId;
    private Long tagId;

    public TaskTagId() {}

    public TaskTagId(Long taskId, Long tagId) {
        this.taskId = taskId;
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskTagId taskTagId = (TaskTagId) o;
        return Objects.equals(taskId, taskTagId.taskId) && Objects.equals(tagId, taskTagId.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, tagId);
    }

}
