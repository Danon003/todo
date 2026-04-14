package ru.danon.spring.ToDo.controllers.validators;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.services.PeopleService;


@Component
@Slf4j
public class PersonValidator implements Validator {
    private final PeopleService peopleService;

    @Autowired
    public PersonValidator(PeopleService peopleService) {
        this.peopleService = peopleService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Person.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Person person = (Person) target;
        if(peopleService.findByUsername(person.getUsername()).isPresent()) {
            log.warn("Попытка регистрации с уже существующим username: {}", person.getUsername());
            errors.rejectValue("username", "", "Username already exists");
        }
    }
}
