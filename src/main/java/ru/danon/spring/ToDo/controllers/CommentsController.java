package ru.danon.spring.ToDo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@Tag(name = "Comments Controller", description = "Управление комментариями к задачам")
@SecurityRequirement(name = "bearerAuth")
public class CommentsController {

    private final CommentsService commentsService;
    private final PeopleService peopleService;
    private final ModelMapper modelMapper;

    @GetMapping
    @Operation(summary = "Получить комментарии к задаче", description = "Возвращает страницу с комментариями для указанной задачи")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение комментариев",
                    content = @Content(schema = @Schema(implementation = CommentDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка получения комментариев")
    })
    public ResponseEntity<Page<CommentDTO>> getComments(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            @Parameter(description = "Параметры пагинации (size, page, sort)")
            @PageableDefault(size = 20) Pageable pageable) {
        try {
            return ResponseEntity.ok(commentsService.getTaskComments(taskId, pageable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    @Operation(summary = "Создать комментарий", description = "Добавляет новый комментарий к задаче")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Комментарий успешно создан",
                    content = @Content(schema = @Schema(implementation = CommentDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка создания комментария")
    })
    public ResponseEntity<CommentDTO> createComment(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            @Parameter(description = "Данные комментария", required = true)
            @RequestBody CommentDTO commentDTO,
            Authentication auth) {

        try {
            return ResponseEntity.ok(commentsService.addComment(taskId, auth, commentDTO));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "Обновить комментарий", description = "Редактирует существующий комментарий (только автор)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Комментарий успешно обновлен",
                    content = @Content(schema = @Schema(implementation = CommentDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка обновления комментария (недостаточно прав или комментарий не найден)")
    })
    public ResponseEntity<CommentDTO> updateComment(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            @Parameter(description = "ID комментария", required = true)
            @PathVariable String commentId,
            @Parameter(description = "Содержимое комментария", required = true)
            @RequestBody CommentRequest request,
            Authentication authentication) {

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
    @Operation(summary = "Удалить комментарий", description = "Удаляет комментарий (автор или преподаватель/админ)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Комментарий успешно удален"),
            @ApiResponse(responseCode = "400", description = "Ошибка удаления комментария (недостаточно прав или комментарий не найден)")
    })
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            @Parameter(description = "ID комментария", required = true)
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
    @Operation(summary = "Получить ответы на комментарий", description = "Возвращает список ответов на указанный комментарий")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение ответов",
                    content = @Content(schema = @Schema(implementation = CommentDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка получения ответов")
    })
    public ResponseEntity<List<CommentDTO>> getCommentReplies(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Integer taskId,
            @Parameter(description = "ID комментария", required = true)
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