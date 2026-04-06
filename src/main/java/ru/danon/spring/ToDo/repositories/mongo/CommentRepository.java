package ru.danon.spring.ToDo.repositories.mongo;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.Comment;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends MongoRepository<Comment, Integer> {

    Optional<Comment> findById(String id);

    Page<Comment> findByTaskIdAndParentIdIsNullOrderByCreatedAtAsc(Integer taskId, Pageable pageable);

    List<Comment> findByParentIdOrderByCreatedAtAsc(String parentId);

    List<Comment> findByParentId(String commentId);
}
