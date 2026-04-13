package ru.danon.spring.ToDo.models.postgre;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ru.danon.spring.ToDo.models.postgre.id.TaskTagId;

@Getter
@Setter
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
}