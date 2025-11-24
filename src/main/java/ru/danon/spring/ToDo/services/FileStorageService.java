package ru.danon.spring.ToDo.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final S3Client s3Client;

    @Value("${app.storage.bucket-name}")
    private String bucketName;

    @Value("${spring.cloud.aws.s3.endpoint}")
    private String minioEndpoint;

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;


    public String uploadFile(MultipartFile file, String filePath) {
        try {
            // Проверяем размер файла
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new RuntimeException("Файл слишком большой. Максимальный размер: 10MB");
            }

            validateFileSafety(file);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("Файл загружен в MinIO: {}", filePath);
            return filePath;

        } catch (IOException e) {
            log.error("Ошибка при загрузке файла в MinIO", e);
            throw new RuntimeException("Не удалось загрузить файл", e);
        }
    }

    public String generateDownloadUrl(String filePath) {
        try {
            try {
                s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(filePath)
                        .build());
            } catch (NoSuchKeyException e) {
                throw new RuntimeException("Файл не найден: " + filePath);
            }

            //создаем presigner с теми же настройками что и s3Client
            S3Presigner presigner = S3Presigner.builder()
                    .region(Region.of("us-east-1"))
                    .endpointOverride(URI.create(minioEndpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)
                    ))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build())
                    .build();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();

            presigner.close();

            return url;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Не удалось сгенерировать ссылку для скачивания", e);
        }
    }

    public void deleteFile(String filePath) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Файл удален из MinIO: {}", filePath);
        } catch (Exception e) {
            log.error("Ошибка при удалении файла из MinIO: {}", filePath, e);
            throw new RuntimeException("Не удалось удалить файл", e);
        }
    }

    public String generateFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }

    /**
     * Проверяет безопасность файла перед загрузкой
     */
    public void validateFileSafety(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Файл пустой");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new RuntimeException("Имя файла не может быть пустым");
        }

        // Проверяем расширение файла
        String extension = originalFileName.toLowerCase();
        if (extension.lastIndexOf(".") != -1) {
            extension = extension.substring(extension.lastIndexOf("."));
        }

        // Опасные расширения
        String[] dangerousExtensions = {
                ".exe", ".bat", ".cmd", ".sh", ".bin", ".app", ".jar",
                ".msi", ".com", ".scr", ".pif", ".application", ".gadget",
                ".msc", ".msp", ".hta", ".cpl", ".msh", ".msh1", ".msh2",
                ".mshxml", ".msh1xml", ".msh2xml", ".ps1", ".ps1xml", ".ps2",
                ".ps2xml", ".psc1", ".psc2", ".scf", ".lnk", ".inf", ".reg"
        };

        for (String dangerousExt : dangerousExtensions) {
            if (extension.equals(dangerousExt)) {
                throw new RuntimeException("Загрузка файлов с расширением " + extension + " запрещена");
            }
        }

        // Проверяем MIME type
        String mimeType = file.getContentType();
        if (mimeType != null) {
            String[] dangerousMimeTypes = {
                    "application/x-msdownload",
                    "application/x-ms-installer",
                    "application/x-dosexec",
                    "application/x-executable",
                    "application/x-shellscript"
            };

            for (String dangerousMime : dangerousMimeTypes) {
                if (mimeType.contains(dangerousMime)) {
                    throw new RuntimeException("Загрузка файлов типа " + mimeType + " запрещена");
                }
            }
        }

        // Проверяем размер файла
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("Файл слишком большой. Максимальный размер: 10MB");
        }
    }
}