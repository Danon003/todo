package ru.danon.spring.ToDo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.models.Group;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.models.RoleAuditLog;
import ru.danon.spring.ToDo.repositories.GroupRepository;
import ru.danon.spring.ToDo.repositories.RoleAuditLogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    private final PeopleService peopleService;
    private final GroupRepository groupRepository;
    private final RoleAuditLogRepository roleAuditLogRepository;

    @Autowired
    public AdminService(PeopleService peopleService, PeopleService peopleService1, GroupRepository groupRepository, RoleAuditLogRepository roleAuditLogRepository) {
        this.peopleService = peopleService1;
        this.groupRepository = groupRepository;
        this.roleAuditLogRepository = roleAuditLogRepository;

    }

    public void doAdminStuff(){
        System.out.println("Only admin here");
    }

    public List<Person> getAllUsers() {
        return peopleService.findAll();
    }

    public void createGroup(String groupName, String description) {

        Group group = new Group();
        group.setName(groupName);
        group.setDescription(description);
        group.setCreatedAt(LocalDateTime.now());

        groupRepository.save(group);
    }

    public void changeUserRole(Integer userId, String newRole) {
        Person user = peopleService.findById(userId).orElseThrow(
                () -> new RuntimeException("User not found"));

        RoleAuditLog log = new RoleAuditLog();
        log.setUser(user);
        log.setOldRole(user.getRole());
        log.setNewRole(newRole);
        log.setChangedAt(LocalDateTime.now());

        user.setRole(newRole);
        peopleService.save(user);

        roleAuditLogRepository.save(log);
    }

    public void deleteUser(Integer userId) {
        peopleService.deleteById(userId);
    }


    public Optional<Person> findById(Integer userId) {
        return peopleService.findById(userId);
    }

    @Transactional
    public Person createTeacher(Integer id) {
        Person teacher = peopleService.findById(id).orElseThrow(
                () -> new RuntimeException("User not found"));

        RoleAuditLog log = new RoleAuditLog();
        log.setUser(teacher);
        log.setOldRole(teacher.getRole());
        log.setNewRole("ROLE_TEACHER");
        log.setChangedAt(LocalDateTime.now());


        teacher.setRole("ROLE_TEACHER");
        teacher.setCreatedAt(LocalDateTime.now());

        roleAuditLogRepository.save(log);

        return peopleService.save(teacher);
    }

    public List<Person> getUsersByRole(String role) {
        return peopleService.findByRole("ROLE_" + role);
    }
}
