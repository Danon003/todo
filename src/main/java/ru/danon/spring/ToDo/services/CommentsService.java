package ru.danon.spring.ToDo.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.CommentDTO;
import ru.danon.spring.ToDo.models.mongo.Comment;

import java.util.List;

public interface CommentsService {
    Page<CommentDTO> getTaskComments(Integer taskId, Pageable pageable);

    @Transactional
    CommentDTO addComment(Integer taskId, Authentication auth, CommentDTO commentDTO);

    @Transactional
    CommentDTO updateComment(String commentId, String content, Integer id);

    @Transactional
    void deleteComment(String commentId, Integer id, String role);

    List<Comment> getCommentReplies(String parentId);

    CommentDTO convertToCommentDTO(Comment comment);
}
