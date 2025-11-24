package ru.danon.spring.ToDo.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.models.TaskFile;
import ru.danon.spring.ToDo.repositories.jpa.TaskFileRepository;
import ru.danon.spring.ToDo.repositories.jpa.TaskRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskFileService {

    private final TaskFileRepository taskFileRepository;
    private final FileStorageService fileStorageService;
    private final PeopleService peopleService;
    private final TaskRepository taskRepository;

    @Transactional
    public TaskFile uploadTaskFile(Integer taskId, MultipartFile file, Authentication authentication) {
        Person user = peopleService.findByUsername(authentication.getName()).orElseThrow(
                () -> new RuntimeException("User not found"));

        // Генерируем путь для файла
        String storedFileName = fileStorageService.generateFileName(file.getOriginalFilename());
        String filePath = String.format("tasks/%d/task-files/%s", taskId, storedFileName);

        // Загружаем в MinIO
        fileStorageService.uploadFile(file, filePath);

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

        return taskFileRepository.save(taskFile);
    }

    public List<TaskFile> getTaskFiles(Integer taskId) {
        return taskFileRepository.findByTaskId(taskId);
    }

    @Transactional
    public void deleteTaskFile(Integer fileId) {
        TaskFile taskFile = taskFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        // Удаляем из MinIO
        fileStorageService.deleteFile(taskFile.getFilePath());

        // Удаляем из БД
        taskFileRepository.delete(taskFile);
    }

    public String getFileDownloadUrl(Integer fileId) {
        TaskFile taskFile = taskFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));
        return fileStorageService.generateDownloadUrl(taskFile.getFilePath());
    }
}