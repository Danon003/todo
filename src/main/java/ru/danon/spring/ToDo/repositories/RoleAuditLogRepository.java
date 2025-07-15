package ru.danon.spring.ToDo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.RoleAuditLog;

@Repository
public interface RoleAuditLogRepository extends JpaRepository<RoleAuditLog, Integer> {
}
