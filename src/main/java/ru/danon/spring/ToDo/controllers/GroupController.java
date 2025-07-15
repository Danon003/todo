package ru.danon.spring.ToDo.controllers;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.danon.spring.ToDo.dto.GroupResponseDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.dto.TaskResponseDTO;
import ru.danon.spring.ToDo.models.Group;
import ru.danon.spring.ToDo.services.AdminService;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.TaskService;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/group")
public class GroupController {

    private final GroupService groupService;
    private final AdminService adminService;
    private final TaskService taskService;

    @Autowired
    public GroupController(GroupService groupService, AdminService adminService, TaskService taskService) {
        this.groupService = groupService;
        this.adminService = adminService;
        this.taskService = taskService;
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    @GetMapping()
    public List<GroupResponseDTO> getAllGroups(){
        return groupService.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping()
    public ResponseEntity<Group> createGroup(
            @RequestParam String name,
            @RequestParam(required = false) String description) {

        adminService.createGroup(name, description);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/{groupId}/students")
    public ResponseEntity<List<PersonResponseDTO>> studentsGroup(@PathVariable Integer groupId){

        return ResponseEntity.ok(groupService.getStudentsByGroupId(groupId));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @PostMapping("/{groupId}/students/{studentId}")
    public ResponseEntity<Void> addStudentToGroup(
            @PathVariable Integer groupId,
            @PathVariable Integer studentId) {

        groupService.addStudentToGroup(groupId, studentId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @DeleteMapping("/{groupId}/students/{studentId}")
    public ResponseEntity<Void> deleteStudentFromGroup(
            @PathVariable Integer groupId,
            @PathVariable Integer studentId) {

        groupService.removeStudentFromGroup(groupId, studentId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Integer groupId) {

        groupService.removeGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponseDTO> getGroup(@PathVariable Integer groupId) {
        return ResponseEntity.ok(groupService.findById(groupId));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @GetMapping("/{groupId}/tasks")
    public ResponseEntity<Set<TaskResponseDTO>> getGroupTasks(@PathVariable Integer groupId) {
        return ResponseEntity.ok(taskService.getGroupTasks(groupId));
    }
}
