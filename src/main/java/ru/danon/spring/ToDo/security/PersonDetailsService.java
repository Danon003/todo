package ru.danon.spring.ToDo.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.repositories.jpa.PeopleRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class PersonDetailsService implements UserDetailsService {
    private final PeopleRepository peopleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Загрузка пользователя по username: {}", username);

        Optional<Person> person = peopleRepository.findByUsername(username);
        if (person.isEmpty()) {
            log.error("Пользователь {} не найден", username);
            throw new EntityNotFoundException("User not found", username);
        }

        log.debug("Пользователь {} успешно загружен, роль: {}", username, person.get().getRole());
        return new PersonDetails(person.get());
    }
}
