package ru.danon.spring.ToDo.dto;

import lombok.Data;

@Data
public class ReportRequestDTO {
    private String reportType; // STUDENT_PROGRESS, TASK_STATISTICS, GRADES_OVERVIEW, COMPREHENSIVE
    private String period; // LAST_WEEK, LAST_MONTH, LAST_QUARTER, ALL_TIME
    private String groupId; // "all" or specific group ID
    private Boolean includeGrades;
    private Boolean includeComments;
    private Boolean includeDeadlines;
    private Boolean includeProgress;
    private String format; // "excel" or "doc"
}