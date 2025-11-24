package ru.danon.spring.ToDo.controllers;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.danon.spring.ToDo.dto.CommentDTO;
import ru.danon.spring.ToDo.dto.CommentRequest;
import ru.danon.spring.ToDo.models.Comment;
import ru.danon.spring.ToDo.services.CommentsService;
import ru.danon.spring.ToDo.services.PeopleService;

import java.util.List;

@RestController
@RequestMapping("/task/{taskId}/comments")
@RequiredArgsConstructor
public class CommentsController {

    private final CommentsService commentsService;
    private final PeopleService peopleService;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable Integer taskId) {
        try{
            return ResponseEntity.ok(commentsService.getTaskComments(taskId)
                    .stream()
                    .map(this::convertToCommentDTO)
                    .toList());
        } catch (Exception e){
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<CommentDTO> createComment(@PathVariable Integer taskId,
                                                    @RequestBody CommentDTO commentDTO,
                                                    Authentication auth) {

        try{
            return ResponseEntity.ok(commentsService.addComment(taskId, auth, commentDTO));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Integer taskId,
            @PathVariable String commentId,
            @RequestBody CommentRequest request,
            Authentication authentication) {

        System.out.println("UPDATE COMMENT - TaskId: " + taskId + ", CommentId: " + commentId);
        System.out.println("Request content: " + request.getContent());
        System.out.println("Authentication: " + authentication.getName());

        try {
            var currentUser = peopleService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            CommentDTO updatedComment = commentsService.updateComment(
                    commentId,
                    request.getContent(),
                    currentUser.getId()
            );

            return ResponseEntity.ok(updatedComment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Integer taskId,
            @PathVariable String commentId,
            Authentication authentication) {

        System.out.println("DELETE COMMENT - TaskId: " + taskId + ", CommentId: " + commentId);
        System.out.println("Authentication: " + authentication.getName());

        try {
            var currentUser = peopleService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            commentsService.deleteComment(commentId, currentUser.getId(), currentUser.getRole());

            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<CommentDTO>> getCommentReplies(
            @PathVariable Integer taskId,
            @PathVariable String commentId) {

        try {
            List<CommentDTO> replies = commentsService.getCommentReplies(commentId)
                    .stream()
                    .map(this::convertToCommentDTO)
                    .toList();
            return ResponseEntity.ok(replies);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private CommentDTO convertToCommentDTO(Comment comment) {
        return modelMapper.map(comment, CommentDTO.class);
    }

}
