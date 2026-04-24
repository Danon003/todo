package ru.danon.spring.ToDo.repositories.jpa;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.models.postgre.UserGroup;
import ru.danon.spring.ToDo.models.postgre.id.UserGroupId;

import java.util.List;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, UserGroupId> {
    @EntityGraph(attributePaths = {"user", "group"})
    List<UserGroup> findByGroupId(Long groupId);
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    @Modifying
    @Query("DELETE FROM UserGroup ug WHERE ug.group.id = :groupId AND ug.user.id = :userId")
    void deleteByGroupIdAndUserId(@Param("groupId") Long groupId,
                                  @Param("userId") Long userId);

    UserGroup findUserGroupByUser(Person user);

    Integer countByGroupId(Long groupId);

    @Query("Select g.name from Group g where g.id = :groupId")
    String getGroupName(@Param("groupId") Long groupId);
}
