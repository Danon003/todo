package ru.danon.spring.ToDo.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.CommentDTO;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.models.mongo.Comment;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.models.postgre.Task;
import ru.danon.spring.ToDo.models.postgre.TaskAssignment;
import ru.danon.spring.ToDo.repositories.jpa.TaskAssignmentRepository;
import ru.danon.spring.ToDo.repositories.mongo.CommentRepository;
import ru.danon.spring.ToDo.services.CommentsService;
import ru.danon.spring.ToDo.services.NotificationProducerService;
import ru.danon.spring.ToDo.services.PeopleService;
import ru.danon.spring.ToDo.services.TaskService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CommentsServiceImpl implements CommentsService {
    private final CommentRepository commentRepository;
    private final PeopleService peopleService;
    private final ModelMapper modelMapper;
    private final NotificationProducerService notificationProducerServiceImpl;
    private final TaskService taskServiceImpl;
    private final TaskAssignmentRepository taskAssignmentRepository;

    @Override
    public Page<CommentDTO> getTaskComments(Long taskId, Pageable pageable) {
        log.debug("Получение комментариев к задаче id={}, page={}, size={}",
                taskId, pageable.getPageNumber(), pageable.getPageSize());
        return commentRepository
                .findByTaskIdAndParentIdIsNullOrderByCreatedAtAsc(taskId, pageable)
                .map(this::convertToCommentDTO);
    }

    @Transactional
    @Override
    public CommentDTO addComment(Long taskId, Authentication auth, CommentDTO commentDTO) {
        log.info("Добавление комментария к задаче id={} от пользователя: {}", taskId, auth.getName());

        var author = peopleService.findByUsername(auth.getName()).orElseThrow(
                () -> {
                    log.error("Пользователь {} не найден при добавлении комментария", auth.getName());
                    return new EntityNotFoundException("User not found");
                });
        Task task = taskServiceImpl.findTaskById(taskId);

        Comment comment = new Comment(
                taskId,
                author.getId(),
                author.getUsername(),
                author.getRole(),
                commentDTO.getContent());

        if(commentDTO.getParentId() != null) {
            comment.setParentId(commentDTO.getParentId());
            comment.setRepliesCount(comment.getRepliesCount()+1);
            log.debug("Добавление ответа на комментарий id={}", commentDTO.getParentId());
        }
        Comment savedComment = commentRepository.save(comment);
        log.info("Комментарий успешно добавлен: id={}, taskId={}, authorId={}",
                savedComment.getId(), taskId, author.getId());

        if ("STUDENT".equals(author.getRole())) {
            notificationProducerServiceImpl.sendCommentNotification(author.getId(), author.getUsername(), task.getTitle(), taskId);
            log.debug("Уведомление о комментарии студента отправлено");
        } else if ("ROLE_TEACHER".equals(author.getRole())) {
            log.debug("Отправка уведомлений студентам о комментарии преподавателя");
            notifyStudentsAboutTeacherComment(task, author, comment);
        }
        return convertToCommentDTO(comment);
    }

    @Transactional
    @Override
    public CommentDTO updateComment(String commentId, String content, Long id) {
        log.info("Обновление комментария id={} пользователем id={}", commentId, id);

        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> {
            log.error("Комментарий id={} не найден при обновлении", commentId);
            return new EntityNotFoundException("Комментарий", commentId);
        });

        if(!comment.getAuthorId().equals(id)) {
            log.warn("Попытка обновления чужого комментария id={} пользователем id={}", commentId, id);
            throw new AccessDeniedException("You don`t have rights to update this comment");
        }

        comment.setContent(content);
        commentRepository.save(comment);
        log.info("Комментарий id={} успешно обновлен", commentId);

        return convertToCommentDTO(comment);
    }

    @Transactional
    @Override
    public void deleteComment(String commentId, Long id, String role) {
        log.info("Удаление комментария id={} пользователем id={} с ролью {}", commentId, id, role);

        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> {
            log.error("Комментарий id={} не найден при удалении", commentId);
            return new EntityNotFoundException("Комментарий", commentId);
        });
        boolean isAuthor = comment.getAuthorId().equals(id);
        boolean isTeacher = "ROLE_TEACHER".equals(role);

        if(!isAuthor && !isTeacher){
            log.warn("Попытка удаления чужого комментария id={} пользователем id={} с ролью {}",
                    commentId, id, role);
            throw new AccessDeniedException("You don`t have rights to delete this comment");
        }

        List<Comment> replies = commentRepository.findByParentId(commentId);
        commentRepository.deleteAll(replies);
        log.debug("Удалено {} ответов на комментарий id={}", replies.size(), commentId);

        if(comment.getParentId() != null)
            comment.setRepliesCount(comment.getRepliesCount() - 1);
        commentRepository.delete(comment);
        log.info("Комментарий id={} успешно удален", commentId);
    }

    private void notifyStudentsAboutTeacherComment(Task task, Person teacher, Comment comment) {
        List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(task.getId());

        for (TaskAssignment assignment : assignments) {
            Person student = peopleService.findById(assignment.getUserId()).orElseThrow();
            if (!student.getId().equals(teacher.getId())) {
                notificationProducerServiceImpl.sendCommentNotification(
                        student.getId(),
                        teacher.getUsername(),
                        task.getTitle(),
                        task.getId()
                );
            }
        }
        log.debug("Отправлено {} уведомлений студентам о комментарии преподавателя", assignments.size());
    }

    @Override
    public List<Comment> getCommentReplies(String parentId) {
        log.debug("Получение ответов на комментарий id={}", parentId);
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
        log.debug("Получено {} ответов на комментарий id={}", replies.size(), parentId);
        return replies;
    }

    @Override
    public CommentDTO convertToCommentDTO(Comment comment) {
        return modelMapper.map(comment, CommentDTO.class);
    }
}
