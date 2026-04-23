package ru.danon.spring.ToDo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.danon.spring.ToDo.dto.ReportRequestDTO;
import ru.danon.spring.ToDo.services.ReportService;

import java.io.IOException;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Report Controller", description = "Генерация отчетов (DOCX/XLSX)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ReportController {
    private final ReportService reportService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ROLE_TEACHER') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Сгенерировать отчет", description = "Генерирует отчет в формате DOCX или XLSX (доступно для TEACHER и ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Отчет успешно сгенерирован",
                    content = @Content(mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры запроса"),
            @ApiResponse(responseCode =  "403", description = "Доступ запрещен - требуется роль TEACHER или ADMIN"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера при генерации отчета")
    })
    public ResponseEntity<byte[]> generateReport(
            @Parameter(description = "Параметры генерации отчета", required = true)
            @RequestBody ReportRequestDTO request) throws IOException {

        log.info("Запрос на генерацию отчета: тип={}, формат={}", request.getReportType(), request.getFormat());

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

        log.info("Отчет успешно сгенерирован: {}, размер: {} bytes", filename, reportBytes.length);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(reportBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(reportBytes);
    }
}