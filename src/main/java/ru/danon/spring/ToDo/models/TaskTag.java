package ru.danon.spring.ToDo.models;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.danon.spring.ToDo.models.id.TaskTagId;


@Entity
@Table(name = "task_tags")
@IdClass(TaskTagId.class)
public class TaskTag {

    @Id
    @Column(name = "task_id", nullable = false, insertable = false, updatable = false)
    private Integer taskId;

    @Id
    @Column(name = "tag_id", nullable = false, insertable = false, updatable = false)
    private Integer tagId;


    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "task_id")
    private Task task;


    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "tag_id")
    private Tag tag;

    public TaskTag() {}

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
