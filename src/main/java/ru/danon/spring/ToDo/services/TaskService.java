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
import ru.danon.spring.ToDo.dto.TaskPriorityDTO;
import ru.danon.spring.ToDo.dto.TaskDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.dto.TaskStatDTO;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.models.postgre.TaskFile;

import java.util.List;
import java.util.Set;
import java.util.Map;

public interface TaskService {
    @Transactional
    TaskResponseDTO createTask(MyTaskDTO taskDTO, String username);

    //удалить таску
    @Transactional
    void deleteTask(Long taskId);

    //просмотреть все созданные таски
    List<Task> findAllTasks();

    Page<Task> findAllTasks(Pageable pageable);

    //посмотреть конкретную таску
    Task findTaskById(Long taskId);

    //назначить таску юзеру(функция для препода)
    @Transactional
    void assignTask(Long taskId, Long userId, String currentUsername);

    //назначить таску группе по её Id (функция для препода)
    @Transactional
    void assignTaskForGroup(Long taskID, Long groupId, String currentUsername);

    //получить статус таски (функция для препода)
    TaskStatDTO findStatusTask(Long id, Long taskId, String filter);

    Page<MyTaskDTO> findMyTasks(String username, Pageable pageable);

    Page<MyTaskDTO> findUserTasks(Long userId, Pageable pageable);

    //юзер ищет свою конкретную таску
    MyTaskDTO findMyTasksById(Long taskId, String currentUsername);

    //юзер получает статус конкретной таски
    StatusDTO findStatusMyTask(Long taskId, String currentUsername);

    //юзер делится таской с другим юзером
    @Transactional
    void shareTask(Long taskId, Long userId, String currentUsername);

    Set<TaskResponseDTO> getGroupTasks(Long groupId);

    @Transactional
    void updateOverdueTasks();

    List<PersonResponseDTO> getUsersWithTask(Long taskId, Authentication auth);

    @Transactional
    Task updateTask(Long taskId, TaskDTO task, String username);

    @Transactional
    void uploadSolution(Long taskId, MultipartFile file, String username);

    @Transactional
    void gradeSolution(Long taskId, Long studentId, Integer grade, String comment, String username);

    List<TaskFile> getTaskFiles(Long taskId);

    String getSolutionDownloadUrl(Long taskId, String username);

    @Transactional
    void deleteSolution(Long taskId, String username);

    SolutionDTO getStudentSolution(Long taskId, Authentication auth);

    List<SolutionDTO> getAllSolutionsForTask(Long taskId, String teacherUsername);

    String getStudentSolutionDownloadUrl(Long taskId, Long studentId, String teacherUsername);

    Page<MyTaskDTO> findMyActiveTasks(String username, Pageable pageable);

    @Transactional
    int deleteMyOverdueAssignments(String username);

    @Transactional
    MyTaskDTO updateMyTaskPriority(Long taskId, TaskPriorityDTO taskPriorityDTO, String username);

    @Transactional
    Map<String, Integer> assignTasksForGroups(List<Long> taskIds, List<Long> groupIds, String currentUsername);

    Page<Task> findAllActiveTasks(Pageable pageable);
}
