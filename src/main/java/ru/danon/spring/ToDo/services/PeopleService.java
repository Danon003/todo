package ru.danon.spring.ToDo.services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.repositories.jpa.PeopleRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PeopleService {
    private final PeopleRepository peopleRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public PeopleService(PeopleRepository peopleRepository, ModelMapper modelMapper) {
        this.peopleRepository = peopleRepository;
        this.modelMapper = modelMapper;
    }

    public Optional<Person> findByUsername(String username) {
        return peopleRepository.findByUsername(username);
    }

    public List<Person> findAll() {
        return peopleRepository.findAll();
    }

    public Optional<Person> findByEmail(String email) {
        return peopleRepository.findByEmail(email);
    }

    public Optional<Person> findById(Integer userId) {
        return peopleRepository.findById(userId);
    }

    @Transactional
    public void deleteById(Integer userId) {
        peopleRepository.deleteById(userId);
    }

    @Transactional
    public Person save(Person person) {
        return peopleRepository.save(person);
    }

    public List<Person> findByRole(String role) {
        return peopleRepository.findByRole(role);
    }

    public PersonResponseDTO getUserInfo(String name) {
        return convertToPersonResponseDTO(peopleRepository.findByUsername(name));
    }

    private PersonResponseDTO convertToPersonResponseDTO(Optional<Person> byUsername) {
        return modelMapper.map(byUsername.orElse(null), PersonResponseDTO.class);
    }
}
