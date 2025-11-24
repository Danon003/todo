package ru.danon.spring.ToDo.controllers;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.danon.spring.ToDo.dto.GroupResponseDTO;
import ru.danon.spring.ToDo.dto.PersonDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.PeopleService;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final PeopleService peopleService;
    private final GroupService groupService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserController(PeopleService peopleService, GroupService groupService, PasswordEncoder passwordEncoder) {
        this.peopleService = peopleService;
        this.groupService = groupService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me/info")
    public ResponseEntity<PersonResponseDTO> getUserInfo(Authentication authentication) {
        return ResponseEntity.ok(peopleService.getUserInfo(authentication.getName()));
    }

    @GetMapping("/my-group")
    public ResponseEntity<GroupResponseDTO> getMyGroup(Authentication authentication) {
        return ResponseEntity.ok(groupService.getGroupInfo(authentication));
    }
    @GetMapping("/about-user/{id}")
    public Map<String, String> getAboutUser(@PathVariable Integer id) {
        Person person = peopleService.findById(id).orElseThrow(() -> new RuntimeException("Person not found"));
        return Map.of("teacherName", person.getUsername());
    }

    @PutMapping("/me/update")
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody PersonDTO personDTO,
            Authentication authentication) {
        try {
            // Хешируем пароль, если он указан
            String encodedPassword = null;
            if (personDTO.getPassword() != null && !personDTO.getPassword().trim().isEmpty()) {
                encodedPassword = passwordEncoder.encode(personDTO.getPassword());
            }

            PersonResponseDTO updatedUser = peopleService.updateUserProfile(
                    authentication.getName(),
                    personDTO.getUsername(),
                    personDTO.getEmail(),
                    encodedPassword
            );
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
