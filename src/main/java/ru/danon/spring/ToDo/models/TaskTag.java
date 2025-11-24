package ru.danon.spring.ToDo.models;

import jakarta.persistence.*;
import ru.danon.spring.ToDo.models.Tag;
import ru.danon.spring.ToDo.models.Task;
import ru.danon.spring.ToDo.models.id.TaskTagId;

@Entity
@Table(name = "task_tags")
@IdClass(TaskTagId.class)
public class TaskTag {

    @Id
    @Column(name = "task_id", nullable = false)
    private Integer taskId;

    @Id
    @Column(name = "tag_id", nullable = false)
    private Integer tagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", insertable = false, updatable = false)
    private Tag tag;

    public TaskTag() {}

    // Конструктор для удобства
    public TaskTag(Integer taskId, Integer tagId) {
        this.taskId = taskId;
        this.tagId = tagId;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
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