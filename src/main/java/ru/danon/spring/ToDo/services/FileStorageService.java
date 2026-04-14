package ru.danon.spring.ToDo.services;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String uploadFile(MultipartFile file, String filePath);

    String generateDownloadUrl(String filePath);

    void deleteFile(String filePath);

    String generateFileName(String originalFileName);

    void validateFileSafety(MultipartFile file);
}
