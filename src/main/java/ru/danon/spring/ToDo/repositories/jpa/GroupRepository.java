package ru.danon.spring.ToDo.repositories.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.Group;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Integer> {
    @EntityGraph(attributePaths = {"teacher", "userGroups", "userGroups.user"})
    List<Group> findByTeacherId(Integer teacherId);
    @EntityGraph(attributePaths = {"teacher", "userGroups", "userGroups.user"})
    Page<Group> findByTeacherId(Integer teacherId, Pageable pageable);

}
