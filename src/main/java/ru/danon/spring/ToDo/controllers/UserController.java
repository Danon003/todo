package ru.danon.spring.ToDo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.PeopleService;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final PeopleService peopleService;
    private final GroupService groupService;

    @Autowired
    public UserController(PeopleService peopleService, GroupService groupService) {
        this.peopleService = peopleService;
        this.groupService = groupService;
    }

    @GetMapping("/me/info")
    public ResponseEntity<PersonResponseDTO> getUserInfo(Authentication authentication) {
        return ResponseEntity.ok(peopleService.getUserInfo(authentication.getName()));
    }

    @GetMapping("/my-group")
    public Map<String, Integer> getMyGroup(Authentication authentication) {

        return Map.of("id", groupService.getUserGroup(authentication.getName()));
    }
}
