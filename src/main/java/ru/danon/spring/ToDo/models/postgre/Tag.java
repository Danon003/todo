package ru.danon.spring.ToDo.models.postgre;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @NotNull(message = "Имя тега не должно быть пустым")
    @Size(min = 2, max = 50, message = "Название тега должно быть от 2 до 50 символов")
    @Column(name = "name", unique = true)
    private String name;

    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL)
    private List<TaskTag> taskTags;

    public Tag() {}
}
