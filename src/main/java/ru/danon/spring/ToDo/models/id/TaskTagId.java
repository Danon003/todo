package ru.danon.spring.ToDo.models.id;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TaskTagId implements Serializable {
    private Integer taskId;
    private Integer tagId;

    public TaskTagId() {}

    public TaskTagId(Integer taskId, Integer tagId) {
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

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Integer getTagId() {
        return tagId;
    }

    public void setTagId(Integer tagId) {
        this.tagId = tagId;
    }
}
