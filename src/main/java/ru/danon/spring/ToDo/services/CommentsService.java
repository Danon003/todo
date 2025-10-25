package ru.danon.spring.ToDo.services;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.CommentDTO;
import ru.danon.spring.ToDo.models.Comment;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.models.Task;
import ru.danon.spring.ToDo.models.TaskAssignment;
import ru.danon.spring.ToDo.repositories.jpa.TaskAssignmentRepository;
import ru.danon.spring.ToDo.repositories.mongo.CommentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentsService {
    private final CommentRepository commentRepository;
    private final PeopleService peopleService;
    private final ModelMapper modelMapper;
    private final NotificationProducerService notificationProducerService;
    private final TaskService taskService;
    private final TaskAssignmentRepository taskAssignmentRepository;

    public List<Comment> getTaskComments(Integer taskId) {
        return commentRepository.findByTaskIdAndParentIdIsNullOrderByCreatedAtAsc(taskId);
    }

    @Transactional
    public CommentDTO addComment(Integer taskId, Authentication auth, CommentDTO commentDTO) {
        var author = peopleService.findByUsername(auth.getName()).orElseThrow(
                () -> new RuntimeException("User not found"));
        Task task = taskService.findTaskById(taskId);

        Comment comment = new Comment(
                taskId,
                author.getId(),
                author.getUsername(),
                author.getRole(),
                commentDTO.getContent());

        if(commentDTO.getParentId() != null) {
            comment.setParentId(commentDTO.getParentId());
            comment.setRepliesCount(comment.getRepliesCount()+1);
        }
        commentRepository.save(comment);

        if ("STUDENT".equals(author.getRole()))
            notificationProducerService.sendCommentNotification(author.getId(), author.getUsername(), task.getTitle(),taskId);
        else if ("ROLE_TEACHER".equals(author.getRole())) {
            notifyStudentsAboutTeacherComment(task, author, comment);
        }
        return convertToCommentDTO(comment);

    }

    public CommentDTO updateComment(String commentId, String content, Integer id) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found"));
        if(!comment.getAuthorId().equals(id))
            throw new RuntimeException("You don`t have rights to update this comment");

        comment.setContent(content);
        commentRepository.save(comment);

        return convertToCommentDTO(comment);
    }

    public void deleteComment(String commentId, Integer id, String role) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found"));
        boolean isAuthor = comment.getAuthorId().equals(id);
        boolean isTeacher = "ROLE_TEACHER".equals(role);

        if(!isAuthor && !isTeacher){
            throw new RuntimeException("You don`t have rights to delete this comment");
        }

        List<Comment> replies = commentRepository.findByParentId(commentId);
        commentRepository.deleteAll(replies);

        if(comment.getParentId() != null)
            comment.setRepliesCount(comment.getRepliesCount() - 1);
        commentRepository.delete(comment);
    }

    private void notifyStudentsAboutTeacherComment(Task task, Person teacher, Comment comment) {
        List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(task.getId());

        for (TaskAssignment assignment : assignments) {
            Person student = peopleService.findById(assignment.getUserId()).orElseThrow();
            if (!student.getId().equals(teacher.getId())) { // Не отправляем уведомление самому себе
                notificationProducerService.sendCommentNotification(
                        student.getId(),
                        teacher.getUsername(),
                        task.getTitle(),
                        task.getId()
                );
            }
        }
    }

    public List<Comment> getCommentReplies(String parentId) {
        return commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
    }

    public CommentDTO convertToCommentDTO(Comment comment) {
        return modelMapper.map(comment, CommentDTO.class);
    }
}
