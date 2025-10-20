package ru.danon.spring.ToDo.controllers;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.danon.spring.ToDo.dto.TagDTO;
import ru.danon.spring.ToDo.models.Tag;
import ru.danon.spring.ToDo.repositories.TagRepository;
import ru.danon.spring.ToDo.services.TagService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tag")
public class TagController {
        private final TagService tagService;
    private final TagRepository tagRepository;

    @Autowired
        public TagController(TagService tagService, TagRepository tagRepository) {
            this.tagService = tagService;
        this.tagRepository = tagRepository;
    }

        @GetMapping
        public ResponseEntity<List<TagDTO>> getAllTags() {
            List<Tag> tags = tagService.getAllTags();
            List<TagDTO> tagDTOs = tags.stream()
                    .map(tag -> new TagDTO(tag.getId(), tag.getName()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(tagDTOs);
        }

        @PostMapping
        public ResponseEntity<TagDTO> createTag(@RequestBody TagDTO tagDTO) {
            Tag tag = new Tag();
            tag.setName(tagDTO.getName());
            tagService.createTag(tag);
            return ResponseEntity.ok(new TagDTO(tag.getId(), tag.getName()));
        }
}
