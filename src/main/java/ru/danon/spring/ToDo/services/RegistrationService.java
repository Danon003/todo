package ru.danon.spring.ToDo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.events.NotificationEvent;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.repositories.jpa.PeopleRepository;


import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RegistrationService {
    private final PeopleRepository peopleRepository;
    private final NotificationProducerService notificationProducerService;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    public RegistrationService(PeopleRepository peopleRepository, NotificationProducerService notificationProducerService, PasswordEncoder passwordEncoder) {
        this.peopleRepository = peopleRepository;
        this.notificationProducerService = notificationProducerService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(Person person) {
        person.setPassword(passwordEncoder.encode(person.getPassword()));
        person.setRole("ROLE_STUDENT");

        peopleRepository.save(person);

        Person person1 = peopleRepository.findByUsername(person.getUsername()).orElseThrow();
        //уведомление: зарегистрирован новый пользователь
        notificationProducerService.sendNotification(
                new NotificationEvent(
                        UUID.randomUUID().toString(),
                         "REGISTER",
                         "Вы успешно зарегистрировались!",
                        "Спасибо за регистрацию!",
                        person1.getId(),
                        "ROLE_STUDENT",
                        Timestamp.valueOf(LocalDateTime.now()),
                        Map.of("userId", person1.getId())
                )
        );
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
