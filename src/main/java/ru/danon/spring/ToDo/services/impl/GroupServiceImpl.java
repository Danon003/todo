package ru.danon.spring.ToDo.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.GroupResponseDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.exceptions.EntityNotFoundException;
import ru.danon.spring.ToDo.models.postgre.Group;
import ru.danon.spring.ToDo.models.postgre.Person;
import ru.danon.spring.ToDo.models.postgre.UserGroup;
import ru.danon.spring.ToDo.models.postgre.id.UserGroupId;
import ru.danon.spring.ToDo.repositories.jpa.GroupRepository;
import ru.danon.spring.ToDo.repositories.jpa.UserGroupRepository;
import ru.danon.spring.ToDo.security.PersonDetails;
import ru.danon.spring.ToDo.services.GroupService;
import ru.danon.spring.ToDo.services.NotificationProducerService;
import ru.danon.spring.ToDo.services.PeopleService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final PeopleService peopleService;
    private final NotificationProducerService notificationProducerServiceImpl;
    private final ModelMapper modelMapper;

    @Deprecated
    @Override
    public List<GroupResponseDTO> findAll() {
        log.debug("Получение всех групп (устаревший метод)");
        return groupRepository.findAll().stream()
                .map(this::convertToGroupDTO)
                .toList();
    }

    @Override
    public Page<GroupResponseDTO> findAll(Authentication auth, Pageable pageable) {
        PersonDetails personDetails = (PersonDetails) auth.getPrincipal();
        Person person = personDetails.getPerson();
        log.debug("Получение групп для пользователя: {}, роль: {}", person.getUsername(), person.getRole());

        if(isAdmin(person)) {
            Page<Group> groups = groupRepository.findAll(pageable);
            log.debug("ADMIN получает все группы: {} из {}", groups.getNumberOfElements(), groups.getTotalElements());
            return groups.map(this::convertToGroupDTO);
        }
        else if(isTeacher(person)) {
            Page<Group> groups = groupRepository.findByTeacherId(person.getId(), pageable);
            log.debug("TEACHER id={} получает свои группы: {}", person.getId(), groups.getNumberOfElements());
            return groups.map(this::convertToGroupDTO);
        }
        else {
            log.debug("STUDENT не имеет доступа к списку всех групп");
            return Page.empty(pageable);
        }
    }

    @Override
    public List<Person> getPersonsByGroupId(Integer groupId) {
        if (groupId == null) {
            return Collections.emptyList();
        }

        List<Person> users = userGroupRepository.findByGroupId(groupId).stream()
                .map(UserGroup::getUser)
                .filter(Objects::nonNull)
                .toList();

        log.debug("Получено {} пользователей группы id={}", users.size(), groupId);
        return users;
    }

    @Override
    public List<PersonResponseDTO> getStudentsByGroupId(Integer groupId) {
        if (groupId == null) {
            return Collections.emptyList();
        }

        List<Person> users = userGroupRepository.findByGroupId(groupId).stream()
                .map(UserGroup::getUser)
                .filter(Objects::nonNull)
                .toList();

        log.debug("Получено {} студентов группы id={}", users.size(), groupId);
        return convertToResponsePerson(users);
    }

    @Transactional
    @Override
    public void addStudentToGroup(Integer groupId, Integer studentId) {
        log.info("Добавление студента id={} в группу id={}", studentId, groupId);

        UserGroup userGroup = new UserGroup();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> {
                    log.error("Группа id={} не найдена", groupId);
                    return new EntityNotFoundException("Group not found", groupId);
                });

        Person student = peopleService.findById(studentId)
                .orElseThrow(() -> {
                    log.error("Студент id={} не найден", studentId);
                    return new EntityNotFoundException("Student not found", studentId);
                });

        if(userGroupRepository.findUserGroupByUser(student) != null){
            log.warn("Студент id={} уже состоит в группе", studentId);
            throw new IllegalArgumentException(
                    String.format("Студент с id %d уже состоит в группе", studentId)
            );
        }

        userGroup.setId(new UserGroupId(studentId, groupId));
        userGroup.setUser(student);
        userGroup.setGroup(group);
        userGroup.setCreatedAt(LocalDateTime.now());

        userGroupRepository.save(userGroup);
        log.info("Студент id={} успешно добавлен в группу id={}", studentId, groupId);

        //уведомление: вас добавили в группу
        notificationProducerServiceImpl.sendGroupAddedNotification(
                studentId,
                student.getRole(),
                group.getName(),
                groupId
        );
        log.debug("Уведомление о добавлении в группу отправлено студенту id={}", studentId);
    }

    @Transactional
    @Override
    public void removeStudentFromGroup(Integer groupId, Integer studentId) {
        log.info("Удаление студента id={} из группы id={}", studentId, groupId);

        if (!userGroupRepository.existsByGroupIdAndUserId(groupId, studentId)) {
            log.warn("Студент id={} не состоит в группе id={}", studentId, groupId);
            throw new IllegalArgumentException(
                    String.format("Студент с id %d не состоит в группе", studentId)
            );
        }
        userGroupRepository.deleteByGroupIdAndUserId(groupId, studentId);

        Person person = peopleService.findById(studentId)
                .orElseThrow(() -> {
                    log.error("Пользователь id={} не найден", studentId);
                    return new EntityNotFoundException("Person not found", studentId);
                });
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> {
                    log.error("Группа id={} не найдена", groupId);
                    return new EntityNotFoundException("Group not found", groupId);
                });

        log.info("Студент id={} успешно удален из группы id={}", studentId, groupId);

        //уведомление: вас удалили из группы
        notificationProducerServiceImpl.sendGroupRemovedNotification(
                studentId,
                person.getRole(),
                group.getName(),
                groupId
        );
        log.debug("Уведомление об удалении из группы отправлено студенту id={}", studentId);
    }

    @Transactional
    @Override
    public void removeGroup(Integer groupId) {
        log.info("Удаление группы id={}", groupId);
        groupRepository.deleteById(groupId);
        log.info("Группа id={} успешно удалена", groupId);
    }

    @Override
    public GroupResponseDTO findById(Integer groupId, Authentication auth) {
        log.debug("Получение информации о группе id={}", groupId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> {
                    log.error("Группа id={} не найдена", groupId);
                    return new EntityNotFoundException("Group not found", groupId);
                });
        PersonDetails personDetails = (PersonDetails) auth.getPrincipal();
        Person person = personDetails.getPerson();

        if(!hasAccessToGroup(group, person)) {
            log.warn("Пользователь {} не имеет доступа к группе id={}", person.getUsername(), groupId);
            throw new AccessDeniedException("Access denied");
        }

        log.debug("Информация о группе id={} успешно получена", groupId);
        return convertToGroupDTO(group);
    }

    @Override
    public Integer getUserGroup(String name) {
        log.debug("Получение группы пользователя: {}", name);

        try {
            Person user = peopleService.findByUsername(name)
                    .orElse(null);

            if (user == null) {
                log.debug("Пользователь {} не найден", name);
                return null;
            }

            UserGroup userGroup = userGroupRepository.findUserGroupByUser(user);

            Integer groupId = (userGroup != null && userGroup.getGroup() != null)
                    ? userGroup.getGroup().getId()
                    : null;

            log.debug("Группа пользователя {}: {}", name, groupId);
            return groupId;

        } catch (Exception e) {
            log.error("Ошибка при получении группы пользователя {}", name, e);
            return null;
        }
    }

    //метод возвращает юзеров препода (собирает назначенные группы преподу и возвращает их юзеров)
    @Override
    public List<Person> findByTeacherId(Authentication auth) {
        PersonDetails personDetails = (PersonDetails) auth.getPrincipal();
        Person person = personDetails.getPerson();
        log.debug("Получение студентов преподавателя id={}", person.getId());

        List<Person> students = new ArrayList<>();
        List<Group> groups = groupRepository.findByTeacherId(person.getId());

        for (Group group : groups) {
            List<Person> groupStudents = group.getUserGroups().stream()
                    .map(UserGroup::getUser)
                    .toList();
            students.addAll(groupStudents);
        }

        log.debug("Преподаватель id={} имеет {} студентов в {} группах",
                person.getId(), students.size(), groups.size());
        return students;
    }

    @Override
    public List<PersonResponseDTO> getStudentsHasGroup(){
        log.debug("Получение списка студентов, имеющих группу");
        List<UserGroup> studentsId = userGroupRepository.findAll();
        List<Person> students = new ArrayList<>();
        for(UserGroup userGroup : studentsId){
            students.add(userGroup.getUser());
        }
        log.debug("Найдено {} студентов, имеющих группу", students.size());
        return convertToResponsePerson(students);
    }

    private List<PersonResponseDTO> convertToResponsePerson(List<Person> allUsers) {
        return allUsers.stream()
                .map(user -> modelMapper.map(user, PersonResponseDTO.class))
                .collect(Collectors.toList());
    }

    private GroupResponseDTO convertToGroupDTO(Group group) {
        return modelMapper.map(group, GroupResponseDTO.class);
    }

    private boolean isAdmin(Person person) {
        return "ROLE_ADMIN".equals(person.getRole().toUpperCase());
    }

    @Override
    public boolean isTeacher(Person person) {
        return "ROLE_TEACHER".equals(person.getRole().toUpperCase());
    }

    private boolean hasAccessToGroup(Group group, Person person) {
        if(isAdmin(person))
            return true;
        if (isTeacher(person))
            return group.getTeacher()!=null && group.getTeacher().getId().equals(person.getId());

        return userGroupRepository.existsByGroupIdAndUserId(group.getId(), person.getId());
    }

    @Override
    public GroupResponseDTO getGroupInfo(Authentication authentication) {
        log.debug("Получение информации о группе для пользователя: {}", authentication.getName());

        GroupResponseDTO groupResponseDTO = new GroupResponseDTO();
        Integer groupId = getUserGroup(authentication.getName());
        groupResponseDTO.setId(groupId);

        if (groupId != null) {
            groupResponseDTO.setName(userGroupRepository.getGroupName(groupId));
        }

        log.debug("Информация о группе пользователя {}: id={}, name={}",
                authentication.getName(), groupResponseDTO.getId(), groupResponseDTO.getName());
        return groupResponseDTO;
    }
}
