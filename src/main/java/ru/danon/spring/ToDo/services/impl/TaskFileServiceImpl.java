package ru.danon.spring.ToDo.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.models.postgre.TaskFile;
import ru.danon.spring.ToDo.repositories.jpa.TaskFileRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskRepository;
import ru.danon.spring.ToDo.services.FileStorageService;
import ru.danon.spring.ToDo.services.PeopleService;
import ru.danon.spring.ToDo.services.TaskFileService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskFileServiceImpl implements TaskFileService {

    private final TaskFileRepository taskFileRepository;
    private final FileStorageService fileStorageServiceImpl;
    private final PeopleService peopleService;
    private final TaskRepository taskRepository;

    @Transactional
    @Override
    public TaskFile uploadTaskFile(Long taskId, MultipartFile file, Authentication authentication) {
        log.info("Загрузка файла к задаче id={} от пользователя: {}", taskId, authentication.getName());

        Person user = peopleService.findByUsername(authentication.getName()).orElseThrow(
                () -> {
                    log.error("Пользователь {} не найден при загрузке файла", authentication.getName());
                    return new EntityNotFoundException("User not found");
                });

        // Генерируем путь для файла
        String storedFileName = fileStorageServiceImpl.generateFileName(file.getOriginalFilename());
        String filePath = String.format("tasks/%d/task-files/%s", taskId, storedFileName);

        // Загружаем в MinIO
        fileStorageServiceImpl.uploadFile(file, filePath);

        // Сохраняем в БД
        TaskFile taskFile = new TaskFile();
        taskFile.setTask(taskRepository.findTaskById(taskId));
        taskFile.setOriginalFileName(file.getOriginalFilename());
        taskFile.setStoredFileName(storedFileName);
        taskFile.setFilePath(filePath);
        taskFile.setFileSize(file.getSize());
        taskFile.setFileType(file.getContentType());
        taskFile.setUploadedBy(user);
        taskFile.setUploadedAt(LocalDateTime.now());

        TaskFile savedFile = taskFileRepository.save(taskFile);
        log.info("Файл успешно загружен к задаче id={}: fileId={}, name={}", taskId, savedFile.getId(), file.getOriginalFilename());
        return savedFile;
    }

    @Override
    public List<TaskFile> getTaskFiles(Long taskId) {
        log.debug("Получение файлов задачи id={}", taskId);
        List<TaskFile> files = taskFileRepository.findByTaskId(taskId);
        log.debug("Найдено {} файлов для задачи id={}", files.size(), taskId);
        return files;
    }

    @Transactional
    @Override
    public void deleteTaskFile(Long fileId) {
        log.info("Удаление файла задачи id={}", fileId);

        TaskFile taskFile = taskFileRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error("Файл id={} не найден при удалении", fileId);
                    return new EntityNotFoundException("Файл не найден", fileId);
                });

        // Удаляем из MinIO
        fileStorageServiceImpl.deleteFile(taskFile.getFilePath());

        // Удаляем из БД
        taskFileRepository.delete(taskFile);
        log.info("Файл задачи id={} успешно удален", fileId);
    }

    @Override
    public String getFileDownloadUrl(Long fileId) {
        log.debug("Получение ссылки для скачивания файла id={}", fileId);

        TaskFile taskFile = taskFileRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error("Файл id={} не найден при генерации ссылки", fileId);
                    return new EntityNotFoundException("Файл не найден", fileId);
                });

        String downloadUrl = fileStorageServiceImpl.generateDownloadUrl(taskFile.getFilePath());
        log.debug("Ссылка для скачивания файла id={} сгенерирована", fileId);
        return downloadUrl;
    }
}
