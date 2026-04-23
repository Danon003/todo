package ru.danon.spring.ToDo.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.danon.spring.ToDo.enums.FileValidationRules;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.services.FileStorageService;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
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
public class FileStorageServiceImpl implements FileStorageService {

    private final S3Client s3Client;

    @Value("${app.storage.bucket-name}")
    private String bucketName;

    @Value("${spring.cloud.aws.s3.endpoint}")
    private String minioEndpoint;

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;


    @Override
    public String uploadFile(MultipartFile file, String filePath) {
        try {
            log.info("Загрузка файла в MinIO: path={}, size={} bytes", filePath, file.getSize());

            // Проверяем размер файла
            if (file.getSize() > 10 * 1024 * 1024) {
                log.warn("Попытка загрузки слишком большого файла: {} bytes", file.getSize());
                throw new IllegalArgumentException("Файл слишком большой. Максимальный размер: 10MB");
            }

            validateFileSafety(file);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("Файл успешно загружен в MinIO: {}", filePath);
            return filePath;

        } catch (IOException e) {
            log.error("Ошибка при загрузке файла в MinIO: {}", filePath, e);
            throw new RuntimeException("Не удалось загрузить файл", e);
        }
    }

    @Override
    public String generateDownloadUrl(String filePath) {
        log.debug("Генерация ссылки для скачивания файла: {}", filePath);

        try {
            try {
                s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(filePath)
                        .build());
            } catch (NoSuchKeyException e) {
                log.error("Файл не найден в MinIO: {}", filePath);
                throw new EntityNotFoundException("Файл не найден: " + filePath);
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

            log.debug("Ссылка для скачивания сгенерирована: {}", filePath);
            return url;

        } catch (Exception e) {
            log.error("Ошибка при генерации ссылки для скачивания: {}", filePath, e);
            throw new RuntimeException("Не удалось сгенерировать ссылку для скачивания", e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        log.info("Удаление файла из MinIO: {}", filePath);

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Файл успешно удален из MinIO: {}", filePath);
        } catch (Exception e) {
            log.error("Ошибка при удалении файла из MinIO: {}", filePath, e);
            throw new RuntimeException("Не удалось удалить файл", e);
        }
    }

    @Override
    public String generateFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String generatedName = UUID.randomUUID() + extension;
        log.debug("Сгенерировано имя файла: {} -> {}", originalFileName, generatedName);
        return generatedName;
    }

    /**
     * Проверяет безопасность файла перед загрузкой
     */
    @Override
    public void validateFileSafety(MultipartFile file) {
        validateFileNotEmpty(file);
        validateFileName(file.getOriginalFilename());
        validateFileExtension(file.getOriginalFilename());
        validateMimeType(file.getContentType());
        validateFileSize(file.getSize());

        log.debug("Файл прошёл проверку безопасности: name={}, size={} bytes",
                file.getOriginalFilename(), file.getSize());
    }

    private void validateFileNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Попытка загрузки пустого файла");
            throw new IllegalArgumentException("Файл не может быть пустым");
        }
    }

    private void validateFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            log.warn("Попытка загрузки файла без имени");
            throw new IllegalArgumentException("Имя файла не может быть пустым");
        }
    }

    private void validateFileExtension(String originalFilename) {
        String extension = extractExtension(originalFilename);

        if (FileValidationRules.isExtensionDangerous(extension)) {
            log.warn("Попытка загрузки файла с опасным расширением: {}", extension);
            throw new IllegalArgumentException(
                    String.format("Загрузка файлов с расширением '%s' запрещена", extension)
            );
        }
    }

    private void validateMimeType(String mimeType) {
        if (FileValidationRules.isMimeTypeDangerous(mimeType)) {
            log.warn("Попытка загрузки файла с опасным MIME-типом: {}", mimeType);
            throw new IllegalArgumentException(
                    String.format("Загрузка файлов типа '%s' запрещена", mimeType)
            );
        }
    }

    private void validateFileSize(long fileSize) {
        long maxSize = FileValidationRules.MAX_FILE_SIZE.getValue();

        if (fileSize > maxSize) {
            log.warn("Попытка загрузки слишком большого файла: {} bytes, лимит: {} bytes",
                    fileSize, maxSize);
            throw new IllegalArgumentException(
                    String.format("Файл слишком большой. Максимальный размер: %s",
                            FileValidationRules.MAX_FILE_SIZE.getReadableSize())
            );
        }
    }

    private String extractExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex).toLowerCase();
    }
}
