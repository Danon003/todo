package ru.danon.spring.ToDo.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.GroupResponseDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.models.postgre.Person;

import java.util.List;

public interface GroupService {
    @Deprecated
    List<GroupResponseDTO> findAll();

    Page<GroupResponseDTO> findAll(Authentication auth, Pageable pageable);

    List<Person> getPersonsByGroupId(Long groupId);

    List<PersonResponseDTO> getStudentsByGroupId(Long groupId);

    @Transactional
    void addStudentToGroup(Long groupId, Long studentId);

    @Transactional
    void removeStudentFromGroup(Long groupId, Long studentId);

    @Transactional
    void removeGroup(Long groupId);

    GroupResponseDTO findById(Long groupId, Authentication auth);

    Long getUserGroup(String name);

    //метод возвращает юзеров препода (собирает назначенные группы преподу и возвращает их юзеров)
    List<Person> findByTeacherId(Authentication auth);

    List<PersonResponseDTO> getStudentsHasGroup();

    boolean isTeacher(Person person);

    GroupResponseDTO getGroupInfo(Authentication authentication);
}
