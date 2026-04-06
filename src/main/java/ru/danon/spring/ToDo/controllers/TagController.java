package ru.danon.spring.ToDo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.danon.spring.ToDo.dto.TagDTO;
import ru.danon.spring.ToDo.repositories.jpa.TagRepository;
import ru.danon.spring.ToDo.services.TagService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tag")
@Tag(name = "Tag Controller", description = "Управление тегами для задач")
@SecurityRequirement(name = "bearerAuth")
public class TagController {
    private final TagService tagService;
    private final TagRepository tagRepository;

    @Autowired
    public TagController(TagService tagService, TagRepository tagRepository) {
        this.tagService = tagService;
        this.tagRepository = tagRepository;
    }

    @GetMapping
    @Operation(summary = "Получить все теги", description = "Возвращает список всех доступных тегов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка тегов",
                    content = @Content(schema = @Schema(implementation = TagDTO.class)))
    })
    public ResponseEntity<List<TagDTO>> getAllTags() {
        List<ru.danon.spring.ToDo.models.Tag> tags = tagService.getAllTags();
        List<TagDTO> tagDTOs = tags.stream()
                .map(tag -> new TagDTO(tag.getId(), tag.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(tagDTOs);
    }

    @PostMapping
    @Operation(summary = "Создать новый тег", description = "Создает новый тег для задач")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Тег успешно создан",
                    content = @Content(schema = @Schema(implementation = TagDTO.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные тега")
    })
    public ResponseEntity<TagDTO> createTag(
            @Parameter(description = "Данные тега", required = true)
            @RequestBody TagDTO tagDTO) {
        ru.danon.spring.ToDo.models.Tag tag = new ru.danon.spring.ToDo.models.Tag();
        tag.setName(tagDTO.getName());
        tagService.createTag(tag);
        return ResponseEntity.ok(new TagDTO(tag.getId(), tag.getName()));
    }
}