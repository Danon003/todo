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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.danon.spring.ToDo.dto.CommentDTO;
import ru.danon.spring.ToDo.dto.CommentRequest;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.mappers.CommentMapper;
import ru.danon.spring.ToDo.services.CommentsService;
import ru.danon.spring.ToDo.services.PeopleService;

import java.util.List;

@RestController
@RequestMapping("/task/{taskId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments Controller", description = "Управление комментариями к задачам")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class CommentsController {

    private final CommentsService commentsService;
    private final PeopleService peopleService;
    private final CommentMapper commentMapper;

    @GetMapping
    @Operation(summary = "Получить комментарии к задаче", description = "Возвращает страницу с комментариями для указанной задачи")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение комментариев",
                    content = @Content(schema = @Schema(implementation = CommentDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка получения комментариев")
    })
    public ResponseEntity<Page<CommentDTO>> getComments(
            @Parameter(description = "ID задачи", required = true)
            @PathVariable Long taskId,
            @Parameter(description = "Параметры пагинации (size, page, sort)")
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Запрос на получение комментариев к задаче id={}, page={}, size={}", taskId, pageable.getPageNumber(), pageable.getPageSize());
        Page<CommentDTO> comments = commentsService.getTaskComments(taskId, pageable);
        log.debug("Получено {} комментариев к задаче id={}", comments.getNumberOfElements(), taskId);
        return ResponseEntity.ok(comments);
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
            @PathVariable Long taskId,
            @Parameter(description = "Данные комментария", required = true)
            @RequestBody CommentDTO commentDTO,
            Authentication auth) {

        log.info("Запрос на создание комментария к задаче id={} от пользователя: {}", taskId, auth.getName());
        CommentDTO createdComment = commentsService.addComment(taskId, auth, commentDTO);
        log.info("Комментарий успешно создан к задаче id={}, commentId={}", taskId, createdComment.getId());
        return ResponseEntity.ok(createdComment);
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

        log.info("Запрос на обновление комментария id={} к задаче id={} от пользователя: {}", commentId, taskId, authentication.getName());

        var currentUser = peopleService.findByUsername(authentication.getName())
                .orElseThrow(() -> {
                    log.error("Пользователь {} не найден при попытке обновления комментария", authentication.getName());
                    return new RuntimeException("Пользователь не найден");
                });

        CommentDTO updatedComment = commentsService.updateComment(
                commentId,
                request.getContent(),
                currentUser.getId()
        );

        log.info("Комментарий id={} успешно обновлен пользователем id={}", commentId, currentUser.getId());
        return ResponseEntity.ok(updatedComment);
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

        log.info("Запрос на удаление комментария id={} к задаче id={} от пользователя: {}", commentId, taskId, authentication.getName());

        var currentUser = peopleService.findByUsername(authentication.getName())
                .orElseThrow(() -> {
                    log.error("Пользователь {} не найден при попытке удаления комментария", authentication.getName());
                    return new EntityNotFoundException("Пользователь не найден");
                });

        commentsService.deleteComment(commentId, currentUser.getId(), currentUser.getRole());
        log.info("Комментарий id={} успешно удален пользователем id={} с ролью {}", commentId, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
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

        log.info("Запрос на получение ответов к комментарию id={} задачи id={}", commentId, taskId);
        List<CommentDTO> replies = commentsService.getCommentReplies(commentId)
                .stream()
                .map(commentMapper::toDTO)
                .toList();
        log.debug("Получено {} ответов к комментарию id={}", replies.size(), commentId);
        return ResponseEntity.ok(replies);
    }
}