package ru.danon.spring.ToDo.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.DashboardStatsDTO;
import ru.danon.spring.ToDo.dto.LogResponseDTO;
import ru.danon.spring.ToDo.models.postgre.Person;

import java.util.List;

public interface AdminService {
    Page<Person> getAllUsers(Pageable page);

    @Transactional
    void createGroup(String groupName, String description);

    @Transactional
    void changeUserRole(Integer userId, String newRole);

    @Transactional
    void assignTeacherToGroup(Integer groupId, Integer userId);

    @Transactional
    Person createTeacher(Integer id);

    Page<Person> getUsersByRole(String role, Pageable page);

    List<LogResponseDTO> getRoleAuditLogs();

    DashboardStatsDTO getDashboardStats(Authentication auth);
}
