package ru.danon.spring.ToDo.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.danon.spring.ToDo.dto.ReportRequestDTO;
import ru.danon.spring.ToDo.services.ReportService;

import java.io.IOException;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ROLE_TEACHER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<byte[]> generateReport(@RequestBody ReportRequestDTO request) {
        try {
            byte[] reportBytes = reportService.generateReport(request);

            // Определяем тип контента и расширение файла
            String contentType;
            String fileExtension;
            if ("doc".equalsIgnoreCase(request.getFormat())) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                fileExtension = "docx";
            } else {
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                fileExtension = "xlsx";
            }

            // Генерируем имя файла
            String reportType = request.getReportType().toLowerCase().replace("_", "-");
            String timestamp = java.time.LocalDate.now().toString();
            String filename = String.format("report_%s_%s.%s", reportType, timestamp, fileExtension);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(reportBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
