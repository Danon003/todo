package ru.danon.spring.ToDo.models.postgre;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.danon.spring.ToDo.models.postgre.id.UserGroupId;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_groups")
public class UserGroup {

    @EmbeddedId
    private UserGroupId id;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id")
    private Person user;

    @MapsId("groupId")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "group_id")
    private Group group;

    public UserGroup(){}
}
