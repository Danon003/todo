package ru.danon.spring.ToDo.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.repositories.jpa.PeopleRepository;
import ru.danon.spring.ToDo.services.PeopleService;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
@Slf4j
public class PeopleServiceImpl implements PeopleService {
    private final PeopleRepository peopleRepository;
    private final ModelMapper modelMapper;

    @Override
    public Optional<Person> findByUsername(String username) {
        log.debug("Поиск пользователя по username: {}", username);
        return peopleRepository.findByUsername(username);
    }

    @Override
    public Page<Person> findAll(Pageable page) {
        log.debug("Получение всех пользователей с пагинацией: page={}, size={}",
                page.getPageNumber(), page.getPageSize());
        return peopleRepository.findAll(page);
    }

    @Override
    public List<Person> findAll() {
        log.debug("Получение всех пользователей");
        return peopleRepository.findAll();
    }

    @Override
    public Optional<Person> findByEmail(String email) {
        log.debug("Поиск пользователя по email: {}", email);
        return peopleRepository.findByEmail(email);
    }

    @Override
    public Optional<Person> findById(Long userId) {
        log.debug("Поиск пользователя по id: {}", userId);
        return peopleRepository.findById(userId);
    }

    @Transactional
    @Override
    public void deleteById(Long userId) {
        log.info("Удаление пользователя id={}", userId);
        peopleRepository.deleteById(userId);
        log.info("Пользователь id={} успешно удален", userId);
    }

    @Transactional
    @Override
    public Person save(Person person) {
        log.info("Сохранение пользователя: username={}, email={}", person.getUsername(), person.getEmail());
        Person savedPerson = peopleRepository.save(person);
        log.debug("Пользователь сохранен: id={}, username={}", savedPerson.getId(), savedPerson.getUsername());
        return savedPerson;
    }

    @Override
    public List<Person> findByRole(String role) {
        log.debug("Поиск пользователей по роли: {}", role);
        List<Person> users = peopleRepository.findByRole(role);
        log.debug("Найдено {} пользователей с ролью {}", users.size(), role);
        return users;
    }

    @Override
    public Page<Person> findByRole(String role, Pageable page) {
        log.debug("Поиск пользователей по роли с пагинацией: role={}, page={}, size={}",
                role, page.getPageNumber(), page.getPageSize());
        return peopleRepository.findByRole(role, page);
    }

    @Override
    public PersonResponseDTO getUserInfo(String name) {
        log.debug("Получение информации о пользователе: {}", name);
        PersonResponseDTO userInfo = convertToPersonResponseDTO(peopleRepository.findByUsername(name));
        log.debug("Информация о пользователе {} получена", name);
        return userInfo;
    }

    @Transactional
    @Override
    public PersonResponseDTO updateUserProfile(String username, String newUsername, String newEmail, String newPassword) {
        log.info("Обновление профиля пользователя: {}", username);

        Person person = peopleRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Пользователь {} не найден при обновлении профиля", username);
                    return new EntityNotFoundException("Пользователь не найден", username);
                });

        // Проверяем, не занят ли новый email другим пользователем
        if (!person.getEmail().equals(newEmail)) {
            Optional<Person> existingPerson = peopleRepository.findByEmail(newEmail);
            if (existingPerson.isPresent() && !existingPerson.get().getId().equals(person.getId())) {
                log.warn("Email {} уже используется другим пользователем", newEmail);
                throw new IllegalArgumentException("Email уже используется другим пользователем");
            }
        }

        // Проверяем, не занят ли новый username другим пользователем
        if (!person.getUsername().equals(newUsername)) {
            Optional<Person> existingPerson = peopleRepository.findByUsername(newUsername);
            if (existingPerson.isPresent() && !existingPerson.get().getId().equals(person.getId())) {
                log.warn("Username {} уже занят другим пользователем", newUsername);
                throw new IllegalArgumentException("Имя пользователя уже занято");
            }
        }

        // Обновляем данные
        person.setUsername(newUsername);
        person.setEmail(newEmail);

        // Обновляем пароль только если он указан и не пустой
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            person.setPassword(newPassword);
            log.debug("Пароль пользователя {} обновлен", username);
        }

        Person updatedPerson = peopleRepository.save(person);
        log.info("Профиль пользователя {} успешно обновлен", username);
        return convertToPersonResponseDTO(Optional.of(updatedPerson));
    }

    private PersonResponseDTO convertToPersonResponseDTO(Optional<Person> byUsername) {
        return modelMapper.map(byUsername.orElse(null), PersonResponseDTO.class);
    }
}
