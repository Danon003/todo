package ru.danon.spring.ToDo.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.models.postgre.Person;

import java.util.List;
import java.util.Optional;

public interface PeopleService {
    Optional<Person> findByUsername(String username);

    Page<Person> findAll(Pageable page);

    List<Person> findAll();

    Optional<Person> findByEmail(String email);

    Optional<Person> findById(Long userId);

    @Transactional
    void deleteById(Long userId);

    @Transactional
    Person save(Person person);

    List<Person> findByRole(String role);

    Page<Person> findByRole(String role, Pageable page);

    PersonResponseDTO getUserInfo(String name);

    @Transactional
    PersonResponseDTO updateUserProfile(String username, String newUsername, String newEmail, String newPassword);
}
