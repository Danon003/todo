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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.danon.spring.ToDo.models.postgre.id.TaskAssignmentId;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "task_assignments", schema = "task_tracker")
@IdClass(TaskAssignmentId.class)
public class TaskAssignment {
    @Id
    @Column(name = "task_id", insertable = false, updatable = false)
    private Long taskId;

    @Id
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "task_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Person user;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "assigned_by")
    private Person assignedBy;

    @Column(name = "status")
    private String status = "NOT_STARTED";

    @Column(name = "priority")
    private String priority = "MEDIUM";

    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "updated_at")
    private LocalDateTime updated_At;

    @Column(name = "solution_file_name")
    private String solutionFileName;

    @Column(name = "solution_file_path")
    private String solutionFilePath;

    @Column(name = "solution_file_size")
    private Long solutionFileSize;

    @Column(name = "solution_uploaded_at")
    private LocalDateTime solutionUploadedAt;

    @Column(name = "grade")
    private Integer grade;

    @Column(name = "teacher_comment")
    private String teacherComment;

    public boolean canUploadSolution() {
        return this.task.getDeadline().isAfter(LocalDateTime.now());
    }

    public boolean hasSolution() {
        return this.solutionFilePath != null && !this.solutionFilePath.trim().isEmpty();
    }

}
