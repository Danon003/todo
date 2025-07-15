package ru.danon.spring.ToDo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.repositories.PeopleRepository;


import java.util.List;

@Service
public class RegistrationService {
    private final PeopleRepository peopleRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    public RegistrationService(PeopleRepository peopleRepository, PasswordEncoder passwordEncoder) {
        this.peopleRepository = peopleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(Person person) {
        person.setPassword(passwordEncoder.encode(person.getPassword()));
        person.setRole("ROLE_STUDENT");

        peopleRepository.save(person);
    }

    @Transactional
    public void updatePasswordsToBCrypt() {
        List<Person> persons = peopleRepository.findAll();
        for (Person person : persons) {
            if (!person.getPassword().startsWith("$2a$")) {
                person.setPassword(passwordEncoder.encode(person.getPassword()));
            }
        }
        peopleRepository.saveAll(persons);
    }
}
