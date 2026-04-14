package ru.danon.spring.ToDo.services;

import ru.danon.spring.ToDo.dto.ReportRequestDTO;

import java.io.IOException;

public interface ReportService {
    byte[] generateReport(ReportRequestDTO request) throws IOException;
}
