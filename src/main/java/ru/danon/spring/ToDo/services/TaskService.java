package ru.danon.spring.ToDo.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.danon.spring.ToDo.dto.MyTaskDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.dto.SolutionDTO;
import ru.danon.spring.ToDo.dto.StatusDTO;
import ru.danon.spring.ToDo.dto.TaskDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.dto.TaskStatDTO;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.models.postgre.TaskFile;

import java.util.List;
import java.util.Set;

public interface TaskService {
    @Transactional
    TaskResponseDTO createTask(MyTaskDTO taskDTO, String username);

    //удалить таску
    @Transactional
    void deleteTask(Integer taskId);

    //просмотреть все созданные таски
    List<Task> findAllTasks();

    Page<Task> findAllTasks(Pageable pageable);

    //посмотреть конкретную таску
    Task findTaskById(Integer taskId);

    //назначить таску юзеру(функция для препода)
    @Transactional
    void assignTask(Integer taskId, Integer userId, String currentUsername);

    //назначить таску группе по её Id (функция для препода)
    @Transactional
    void assignTaskForGroup(Integer taskID, Integer groupId, String currentUsername);

    //получить статус таски (функция для препода)
    TaskStatDTO findStatusTask(Integer id, Integer taskId, String filter);

    Page<MyTaskDTO> findMyTasks(String username, Pageable pageable);

    Page<MyTaskDTO> findUserTasks(Integer userId, Pageable pageable);

    //юзер ищет свою конкретную таску
    MyTaskDTO findMyTasksById(Integer taskId, String currentUsername);

    //юзер получает статус конкретной таски
    StatusDTO findStatusMyTask(Integer taskId, String currentUsername);

    //юзер меняет статус конкретной таски на переданный status
    @Transactional
    MyTaskDTO changeMyTask(Integer taskId, String status, String currentUsername);

    //юзер делится таской с другим юзером
    @Transactional
    void shareTask(Integer taskId, Integer userId, String currentUsername);

    Set<TaskResponseDTO> getGroupTasks(Integer groupId);

    @Transactional
    void updateOverdueTasks();

    List<PersonResponseDTO> getUsersWithTask(Integer taskId, Authentication auth);

    @Transactional
    Task updateTask(Integer taskId, TaskDTO task, String username);

    @Transactional
    void uploadSolution(Integer taskId, MultipartFile file, String username);

    @Transactional
    void gradeSolution(Integer taskId, Integer studentId, Integer grade, String comment, String username);

    List<TaskFile> getTaskFiles(Integer taskId);

    String getSolutionDownloadUrl(Integer taskId, String username);

    @Transactional
    void deleteSolution(Integer taskId, String username);

    SolutionDTO getStudentSolution(Integer taskId, Authentication auth);

    List<SolutionDTO> getAllSolutionsForTask(Integer taskId, String teacherUsername);

    String getStudentSolutionDownloadUrl(Integer taskId, Integer studentId, String teacherUsername);
}
