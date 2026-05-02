package ru.danon.spring.ToDo.dto;

import lombok.Data;

@Data
public class TagDTO {
    private Long id;
    private String name;

    public TagDTO() {}

    public TagDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
