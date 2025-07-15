package ru.danon.spring.ToDo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.models.UserGroup;
import ru.danon.spring.ToDo.models.id.UserGroupId;

import java.util.List;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, UserGroupId> {
    List<UserGroup> findByGroupId(Integer groupId);
    boolean existsByGroupIdAndUserId(Integer groupId, Integer userId);

    @Modifying
    @Query("DELETE FROM UserGroup ug WHERE ug.group.id = :groupId AND ug.user.id = :userId")
    void deleteByGroupIdAndUserId(@Param("groupId") Integer groupId,
                                  @Param("userId") Integer userId);

    UserGroup findUserGroupByUser(Person user);
}
