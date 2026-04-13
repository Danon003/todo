package ru.danon.spring.ToDo.dto;

import lombok.Data;

@Data
public class TagDTO {
    private Integer id;
    private String name;

    public TagDTO() {}

    public TagDTO(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
}
