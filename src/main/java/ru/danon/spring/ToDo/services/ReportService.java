package ru.danon.spring.ToDo.services;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.ReportRequestDTO;
import ru.danon.spring.ToDo.models.*;
import ru.danon.spring.ToDo.repositories.jpa.*;
import ru.danon.spring.ToDo.repositories.mongo.CommentRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final GroupRepository groupRepository;
    private final PeopleRepository peopleRepository;
    private final UserGroupRepository userGroupRepository;
    private final CommentRepository commentRepository;

    public byte[] generateReport(ReportRequestDTO request) throws IOException {
        if ("excel".equalsIgnoreCase(request.getFormat()) || request.getFormat() == null) {
            return generateExcelReport(request);
        } else if ("doc".equalsIgnoreCase(request.getFormat())) {
            return generateDocReport(request);
        } else {
            throw new IllegalArgumentException("Unsupported format: " + request.getFormat());
        }
    }

    private byte[] generateExcelReport(ReportRequestDTO request) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Отчет");

        // Стили
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        int rowNum = 0;

        // Заголовок
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(getReportTitle(request));
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        rowNum++; // Пустая строка

        // Генерируем данные в зависимости от типа отчета
        switch (request.getReportType()) {
            case "STUDENT_PROGRESS":
                generateStudentProgressReport(sheet, request, headerStyle, dataStyle, rowNum);
                break;
            case "TASK_STATISTICS":
                generateTaskStatisticsReport(sheet, request, headerStyle, dataStyle, rowNum);
                break;
            case "GRADES_OVERVIEW":
                generateGradesOverviewReport(sheet, request, headerStyle, dataStyle, rowNum);
                break;
            case "COMPREHENSIVE":
                generateComprehensiveReport(sheet, request, headerStyle, dataStyle, rowNum);
                break;
            default:
                throw new IllegalArgumentException("Unknown report type: " + request.getReportType());
        }

        // Автоматическая ширина колонок
        for (int i = 0; i < 10; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    private void generateStudentProgressReport(Sheet sheet, ReportRequestDTO request,
                                               CellStyle headerStyle, CellStyle dataStyle, int startRow) {
        int rowNum = startRow;
        Row headerRow = sheet.createRow(rowNum++);

        List<String> headers = new ArrayList<>(Arrays.asList("Студент", "Группа", "Задача", "Статус"));
        if (request.getIncludeProgress() != null && request.getIncludeProgress()) {
            headers.add("Прогресс");
        }
        if (request.getIncludeDeadlines() != null && request.getIncludeDeadlines()) {
            headers.add("Дедлайн");
        }
        if (request.getIncludeGrades() != null && request.getIncludeGrades()) {
            headers.add("Оценка");
        }
        if (request.getIncludeComments() != null && request.getIncludeComments()) {
            headers.add("Комментарий");
        }

        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }

        List<TaskAssignment> assignments = getFilteredAssignments(request);

        for (TaskAssignment assignment : assignments) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;

            row.createCell(colNum++).setCellValue(assignment.getUser().getUsername());
            row.createCell(colNum++).setCellValue(getGroupName(assignment.getUser().getId()));
            row.createCell(colNum++).setCellValue(assignment.getTask().getTitle());
            row.createCell(colNum++).setCellValue(translateStatus(assignment.getStatus()));

            if (request.getIncludeProgress() != null && request.getIncludeProgress()) {
                row.createCell(colNum++).setCellValue(getProgress(assignment));
            }
            if (request.getIncludeDeadlines() != null && request.getIncludeDeadlines()) {
                row.createCell(colNum++).setCellValue(
                        assignment.getTask().getDeadline() != null ?
                                assignment.getTask().getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) :
                                "Нет дедлайна"
                );
            }
            if (request.getIncludeGrades() != null && request.getIncludeGrades()) {
                row.createCell(colNum++).setCellValue(
                        assignment.getGrade() != null ? assignment.getGrade().toString() : "Нет оценки"
                );
            }
            if (request.getIncludeComments() != null && request.getIncludeComments()) {
                row.createCell(colNum++).setCellValue(
                        assignment.getTeacherComment() != null ? assignment.getTeacherComment() : "Нет комментария"
                );
            }
        }
    }

    private void generateTaskStatisticsReport(Sheet sheet, ReportRequestDTO request,
                                              CellStyle headerStyle, CellStyle dataStyle, int startRow) {
        int rowNum = startRow;
        Row headerRow = sheet.createRow(rowNum++);

        List<String> headers = Arrays.asList("Задача", "Автор", "Дедлайн", "Всего назначений",
                "Выполнено", "В работе", "Не начато", "Просрочено");

        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }

        List<Task> tasks = getFilteredTasks(request);

        for (Task task : tasks) {
            Row row = sheet.createRow(rowNum++);
            List<TaskAssignment> assignments = taskAssignmentRepository.findByTask(task);

            long completed = assignments.stream().filter(ta -> "COMPLETED".equals(ta.getStatus())).count();
            long inProgress = assignments.stream().filter(ta -> "IN_PROGRESS".equals(ta.getStatus())).count();
            long notStarted = assignments.stream().filter(ta -> "NOT_STARTED".equals(ta.getStatus())).count();
            long overdue = assignments.stream().filter(ta -> "OVERDUE".equals(ta.getStatus())).count();

            int colNum = 0;
            row.createCell(colNum++).setCellValue(task.getTitle());
            row.createCell(colNum++).setCellValue(task.getAuthor() != null ? task.getAuthor().getUsername() : "Неизвестно");
            row.createCell(colNum++).setCellValue(
                    task.getDeadline() != null ?
                            task.getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) :
                            "Нет дедлайна"
            );
            row.createCell(colNum++).setCellValue(assignments.size());
            row.createCell(colNum++).setCellValue(completed);
            row.createCell(colNum++).setCellValue(inProgress);
            row.createCell(colNum++).setCellValue(notStarted);
            row.createCell(colNum++).setCellValue(overdue);
        }
    }

    private void generateGradesOverviewReport(Sheet sheet, ReportRequestDTO request,
                                              CellStyle headerStyle, CellStyle dataStyle, int startRow) {
        int rowNum = startRow;
        Row headerRow = sheet.createRow(rowNum++);

        List<String> headers = Arrays.asList("Студент", "Группа", "Задача", "Оценка", "Комментарий", "Дата оценки");

        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }

        List<TaskAssignment> assignments = getFilteredAssignments(request)
                .stream()
                .filter(ta -> ta.getGrade() != null)
                .collect(Collectors.toList());

        for (TaskAssignment assignment : assignments) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;

            row.createCell(colNum++).setCellValue(assignment.getUser().getUsername());
            row.createCell(colNum++).setCellValue(getGroupName(assignment.getUser().getId()));
            row.createCell(colNum++).setCellValue(assignment.getTask().getTitle());
            row.createCell(colNum++).setCellValue(assignment.getGrade());
            row.createCell(colNum++).setCellValue(
                    assignment.getTeacherComment() != null ? assignment.getTeacherComment() : "Нет комментария"
            );
            row.createCell(colNum++).setCellValue(
                    assignment.getUpdated_At() != null ?
                            assignment.getUpdated_At().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) :
                            "Не указано"
            );
        }
    }

    private void generateComprehensiveReport(Sheet sheet, ReportRequestDTO request,
                                             CellStyle headerStyle, CellStyle dataStyle, int startRow) {
        // Комплексный отчет включает все данные
        generateStudentProgressReport(sheet, request, headerStyle, dataStyle, startRow);
    }

    private byte[] generateDocReport(ReportRequestDTO request) throws IOException {
        XWPFDocument document = new XWPFDocument();

        // Заголовок
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(getReportTitle(request));
        titleRun.setBold(true);
        titleRun.setFontSize(16);

        // Пустая строка
        document.createParagraph();

        // Генерируем данные в зависимости от типа отчета
        switch (request.getReportType()) {
            case "STUDENT_PROGRESS":
                generateStudentProgressDoc(document, request);
                break;
            case "TASK_STATISTICS":
                generateTaskStatisticsDoc(document, request);
                break;
            case "GRADES_OVERVIEW":
                generateGradesOverviewDoc(document, request);
                break;
            case "COMPREHENSIVE":
                generateComprehensiveDoc(document, request);
                break;
            default:
                throw new IllegalArgumentException("Unknown report type: " + request.getReportType());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.write(outputStream);
        document.close();

        return outputStream.toByteArray();
    }

    private void generateStudentProgressDoc(XWPFDocument document, ReportRequestDTO request) {
        XWPFParagraph para = document.createParagraph();
        XWPFRun run = para.createRun();
        run.setText("Отчет по прогрессу студентов");
        run.setBold(true);
        run.setFontSize(14);

        document.createParagraph();

        List<TaskAssignment> assignments = getFilteredAssignments(request);

        for (TaskAssignment assignment : assignments) {
            para = document.createParagraph();
            run = para.createRun();
            run.setText(String.format("Студент: %s | Группа: %s | Задача: %s | Статус: %s",
                    assignment.getUser().getUsername(),
                    getGroupName(assignment.getUser().getId()),
                    assignment.getTask().getTitle(),
                    translateStatus(assignment.getStatus())
            ));

            if (request.getIncludeGrades() != null && request.getIncludeGrades() && assignment.getGrade() != null) {
                run.addBreak();
                run.setText(String.format("Оценка: %d", assignment.getGrade()));
            }

            if (request.getIncludeComments() != null && request.getIncludeComments() && assignment.getTeacherComment() != null) {
                run.addBreak();
                run.setText(String.format("Комментарий: %s", assignment.getTeacherComment()));
            }

            document.createParagraph();
        }
    }

    private void generateTaskStatisticsDoc(XWPFDocument document, ReportRequestDTO request) {
        XWPFParagraph para = document.createParagraph();
        XWPFRun run = para.createRun();
        run.setText("Статистика по задачам");
        run.setBold(true);
        run.setFontSize(14);

        document.createParagraph();

        List<Task> tasks = getFilteredTasks(request);

        for (Task task : tasks) {
            para = document.createParagraph();
            run = para.createRun();
            List<TaskAssignment> assignments = taskAssignmentRepository.findByTask(task);

            long completed = assignments.stream().filter(ta -> "COMPLETED".equals(ta.getStatus())).count();
            long inProgress = assignments.stream().filter(ta -> "IN_PROGRESS".equals(ta.getStatus())).count();
            long notStarted = assignments.stream().filter(ta -> "NOT_STARTED".equals(ta.getStatus())).count();
            long overdue = assignments.stream().filter(ta -> "OVERDUE".equals(ta.getStatus())).count();

            run.setText(String.format("Задача: %s", task.getTitle()));
            run.addBreak();
            run.setText(String.format("Автор: %s | Дедлайн: %s",
                    task.getAuthor() != null ? task.getAuthor().getUsername() : "Неизвестно",
                    task.getDeadline() != null ?
                            task.getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) :
                            "Нет дедлайна"
            ));
            run.addBreak();
            run.setText(String.format("Всего назначений: %d | Выполнено: %d | В работе: %d | Не начато: %d | Просрочено: %d",
                    assignments.size(), completed, inProgress, notStarted, overdue
            ));

            document.createParagraph();
        }
    }

    private void generateGradesOverviewDoc(XWPFDocument document, ReportRequestDTO request) {
        XWPFParagraph para = document.createParagraph();
        XWPFRun run = para.createRun();
        run.setText("Обзор оценок");
        run.setBold(true);
        run.setFontSize(14);

        document.createParagraph();

        List<TaskAssignment> assignments = getFilteredAssignments(request)
                .stream()
                .filter(ta -> ta.getGrade() != null)
                .collect(Collectors.toList());

        for (TaskAssignment assignment : assignments) {
            para = document.createParagraph();
            run = para.createRun();
            run.setText(String.format("Студент: %s | Группа: %s | Задача: %s | Оценка: %d",
                    assignment.getUser().getUsername(),
                    getGroupName(assignment.getUser().getId()),
                    assignment.getTask().getTitle(),
                    assignment.getGrade()
            ));

            if (assignment.getTeacherComment() != null) {
                run.addBreak();
                run.setText(String.format("Комментарий: %s", assignment.getTeacherComment()));
            }

            document.createParagraph();
        }
    }

    private void generateComprehensiveDoc(XWPFDocument document, ReportRequestDTO request) {
        generateStudentProgressDoc(document, request);
        document.createParagraph();
        generateTaskStatisticsDoc(document, request);
        document.createParagraph();
        generateGradesOverviewDoc(document, request);
    }

    // Вспомогательные методы

    private List<TaskAssignment> getFilteredAssignments(ReportRequestDTO request) {
        LocalDateTime startDate = getStartDate(request.getPeriod());
        List<TaskAssignment> assignments;

        if ("all".equals(request.getGroupId())) {
            assignments = taskAssignmentRepository.findAll();
        } else {
            Integer groupId = Integer.parseInt(request.getGroupId());
            List<Person> students = userGroupRepository.findByGroupId(groupId)
                    .stream()
                    .map(UserGroup::getUser)
                    .collect(Collectors.toList());

            assignments = new ArrayList<>();
            for (Person student : students) {
                assignments.addAll(taskAssignmentRepository.findByUser(student));
            }
        }

        if (startDate != null) {
            assignments = assignments.stream()
                    .filter(ta -> ta.getAssignedAt() != null && ta.getAssignedAt().isAfter(startDate))
                    .collect(Collectors.toList());
        }

        return assignments;
    }

    private List<Task> getFilteredTasks(ReportRequestDTO request) {
        LocalDateTime startDate = getStartDate(request.getPeriod());
        List<Task> tasks = taskRepository.findAll();

        if (startDate != null) {
            tasks = tasks.stream()
                    .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(startDate))
                    .collect(Collectors.toList());
        }

        if (!"all".equals(request.getGroupId())) {
            Integer groupId = Integer.parseInt(request.getGroupId());
            Set<Task> groupTasks = new HashSet<>();
            List<Person> students = userGroupRepository.findByGroupId(groupId)
                    .stream()
                    .map(UserGroup::getUser)
                    .collect(Collectors.toList());

            for (Person student : students) {
                taskAssignmentRepository.findByUser(student).stream()
                        .map(TaskAssignment::getTask)
                        .forEach(groupTasks::add);
            }
            tasks = tasks.stream().filter(groupTasks::contains).collect(Collectors.toList());
        }

        return tasks;
    }

    private LocalDateTime getStartDate(String period) {
        if (period == null || "ALL_TIME".equals(period)) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        return switch (period) {
            case "LAST_WEEK" -> now.minusWeeks(1);
            case "LAST_MONTH" -> now.minusMonths(1);
            case "LAST_QUARTER" -> now.minusMonths(3);
            default -> null;
        };
    }

    private String getGroupName(Integer userId) {
        try {
            UserGroup userGroup = userGroupRepository.findUserGroupByUser(
                    peopleRepository.findById(userId).orElse(null)
            );
            return userGroup != null ? userGroup.getGroup().getName() : "Нет группы";
        } catch (Exception e) {
            return "Нет группы";
        }
    }

    private String translateStatus(String status) {
        return switch (status) {
            case "NOT_STARTED" -> "Не начато";
            case "IN_PROGRESS" -> "В работе";
            case "COMPLETED" -> "Выполнено";
            case "OVERDUE" -> "Просрочено";
            default -> status;
        };
    }

    private String getProgress(TaskAssignment assignment) {
        if ("COMPLETED".equals(assignment.getStatus())) {
            return "100%";
        } else if ("IN_PROGRESS".equals(assignment.getStatus())) {
            return "50%";
        } else if ("NOT_STARTED".equals(assignment.getStatus())) {
            return "0%";
        } else {
            return "0%";
        }
    }

    private String getReportTitle(ReportRequestDTO request) {
        String type = switch (request.getReportType()) {
            case "STUDENT_PROGRESS" -> "Прогресс студентов";
            case "TASK_STATISTICS" -> "Статистика по задачам";
            case "GRADES_OVERVIEW" -> "Обзор оценок";
            case "COMPREHENSIVE" -> "Комплексный отчет";
            default -> "Отчет";
        };

        String period = switch (request.getPeriod()) {
            case "LAST_WEEK" -> "за последнюю неделю";
            case "LAST_MONTH" -> "за последний месяц";
            case "LAST_QUARTER" -> "за последний квартал";
            case "ALL_TIME" -> "за все время";
            default -> "";
        };

        return String.format("%s %s", type, period);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
}
