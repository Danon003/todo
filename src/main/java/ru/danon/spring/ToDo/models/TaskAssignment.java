package ru.danon.spring.ToDo.models;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.danon.spring.ToDo.models.id.TaskAssignmentId;


import java.time.LocalDateTime;

@Entity
@Table(name = "task_assignments")
@IdClass(TaskAssignmentId.class)
public class TaskAssignment {
    @Id
    @Column(name = "task_id", insertable = false, updatable = false)
    private Integer taskId;

    @Id
    @Column(name = "user_id", insertable = false, updatable = false)
    private Integer userId;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "task_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Task task;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Person user;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "assigned_by")
    private Person assignedBy;

    @Column(name = "status")
    private String status = "NOT_STARTED";

    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "updated_at", updatable = false)
    private LocalDateTime updated_At;

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Person getUser() {
        return user;
    }

    public void setUser(Person user) {
        this.user = user;
    }

    public Person getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(Person assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUpdated_At() {
        return updated_At;
    }

    public void setUpdated_At(LocalDateTime updated_At) {
        this.updated_At = updated_At;
    }
}
