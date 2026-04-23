package ru.danon.spring.ToDo.models.postgre.id;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class TaskAssignmentId implements Serializable {
    private Integer taskId;
    private Integer userId;

    public TaskAssignmentId() {}

    public TaskAssignmentId(Integer taskId, Integer userId) {
        this.taskId = taskId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskAssignmentId that = (TaskAssignmentId) o;
        return Objects.equals(taskId, that.taskId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, userId);
    }
}