package ru.danon.spring.ToDo.services;

import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.danon.spring.ToDo.models.postgre.TaskFile;

import java.util.List;

public interface TaskFileService {
    @Transactional
    TaskFile uploadTaskFile(Integer taskId, MultipartFile file, Authentication authentication);

    List<TaskFile> getTaskFiles(Integer taskId);

    @Transactional
    void deleteTaskFile(Integer fileId);

    String getFileDownloadUrl(Integer fileId);
}
