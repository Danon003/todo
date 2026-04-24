package ru.danon.spring.ToDo.services;

import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.danon.spring.ToDo.models.postgre.TaskFile;

import java.util.List;

public interface TaskFileService {
    @Transactional
    TaskFile uploadTaskFile(Long taskId, MultipartFile file, Authentication authentication);

    List<TaskFile> getTaskFiles(Long taskId);

    @Transactional
    void deleteTaskFile(Long fileId);

    String getFileDownloadUrl(Long fileId);
}
