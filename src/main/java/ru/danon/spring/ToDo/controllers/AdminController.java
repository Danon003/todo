package ru.danon.spring.ToDo.controllers;

import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.danon.spring.ToDo.dto.*;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.models.RoleAuditLog;
import ru.danon.spring.ToDo.services.AdminService;
import ru.danon.spring.ToDo.services.PeopleService;

import javax.management.relation.Role;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final PeopleService peopleService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Autowired
    public AdminController(AdminService adminService, PeopleService peopleService, PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
        this.adminService = adminService;
        this.peopleService = peopleService;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @GetMapping("/users")
    public List<PersonResponseDTO> getAllUsers() {
        return convertToResponsePerson(adminService.getAllUsers());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody PersonDTO personDTO) {
        if (peopleService.findByEmail(personDTO.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already in use");
        }

        Person person = convertToPerson(personDTO);
        person.setPassword(passwordEncoder.encode(person.getPassword()));
        person.setRole("ROLE_STUDENT");
        person.setCreatedAt(LocalDateTime.now());

        Person savedPerson = peopleService.save(person);
        return ResponseEntity.ok(savedPerson);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{userId}/delete")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer userId) {
        peopleService.deleteById(userId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{userId}/role")
    public ResponseEntity<Void> changeUserRole(
            @RequestParam String role,
            @PathVariable Integer userId) {

        adminService.changeUserRole(userId, "ROLE_" + role.toUpperCase());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/teachers")
    public ResponseEntity<Person> createTeachers(@RequestBody IdDTO id) {
        return ResponseEntity.ok(adminService.createTeacher(id.getId()));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @GetMapping("/users/by-role")
    public ResponseEntity<List<PersonResponseDTO>> getUserByRole(@RequestParam String role) {
        return ResponseEntity.ok(convertToResponsePerson(adminService.getUsersByRole(role)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/role-audit-log")
    public ResponseEntity<List<LogResponseDTO>> getRoleAuditLog() {
        return ResponseEntity.ok(adminService.getRoleAuditLogs());
    }

    @GetMapping("/statistic")
    public ResponseEntity<DashboardStatsDTO> getStatistic(Authentication auth ){
        return ResponseEntity.ok(adminService.getDashboardStats(auth));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{groupId}/teacher/{teacherId}")
    public ResponseEntity<Void> assignTeacherToGroup(
            @PathVariable Integer groupId,
            @PathVariable Integer teacherId) {

        adminService.assignTeacherToGroup(groupId, teacherId);
        return ResponseEntity.noContent().build();
    }

    private Person convertToPerson(PersonDTO personDTO) {
        return modelMapper.map(personDTO, Person.class);
    }

    private List<PersonResponseDTO> convertToResponsePerson(List<Person> allUsers) {
        return allUsers.stream()
                .map(user -> modelMapper.map(user, PersonResponseDTO.class))
                .collect(Collectors.toList());

    }
}
