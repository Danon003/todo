package ru.danon.spring.ToDo.services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.danon.spring.ToDo.dto.GroupResponseDTO;
import ru.danon.spring.ToDo.dto.PersonResponseDTO;
import ru.danon.spring.ToDo.models.Group;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.models.UserGroup;
import ru.danon.spring.ToDo.models.id.UserGroupId;
import ru.danon.spring.ToDo.repositories.GroupRepository;
import ru.danon.spring.ToDo.repositories.UserGroupRepository;
import ru.danon.spring.ToDo.security.PersonDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final PeopleService peopleService;
    private final NotificationProducerService notificationProducerService;
    private final ModelMapper modelMapper;

    @Autowired
    public GroupService(GroupRepository groupRepository, UserGroupRepository userGroupRepository, PeopleService peopleService, NotificationProducerService notificationProducerService, ModelMapper modelMapper) {
        this.groupRepository = groupRepository;
        this.userGroupRepository = userGroupRepository;
        this.peopleService = peopleService;
        this.notificationProducerService = notificationProducerService;
        this.modelMapper = modelMapper;
    }

    @Deprecated
    public List<GroupResponseDTO> findAll() {
        return groupRepository.findAll().stream()
                .map(this::convertToGroupDTO)
                .toList();
    }

    public List<GroupResponseDTO> findAll(Authentication auth) {
        PersonDetails personDetails = (PersonDetails) auth.getPrincipal();
        Person person = personDetails.getPerson();

        if(isAdmin(person))
            return groupRepository.findAll().stream()
                    .map(this::convertToGroupDTO)
                    .toList();
        else if(isTeacher(person))
            return groupRepository.findByTeacherId(person.getId()).stream()
                    .map(this::convertToGroupDTO)
                    .toList();
        else
            return Collections.emptyList();
    }

    public List<GroupResponseDTO> findAllForAdmin() {
        return groupRepository.findAll().stream()
                .map(this::convertToGroupDTO)
                .toList();
    }

    public List<Person> getPersonsByGroupId(Integer groupId) {
        if (groupId == null) {
            return Collections.emptyList();
        }

        List<Person> users = userGroupRepository.findByGroupId(groupId).stream()
                .map(UserGroup::getUser)
                .filter(Objects::nonNull)
                .toList();

        return users;
    }

    public List<PersonResponseDTO> getStudentsByGroupId(Integer groupId) {
        if (groupId == null) {
            return Collections.emptyList();
        }

        List<Person> users = userGroupRepository.findByGroupId(groupId).stream()
                .map(UserGroup::getUser)
                .filter(Objects::nonNull)
                .toList();

        return convertToResponsePerson(users);
    }

    @Transactional
    public void addStudentToGroup(Integer groupId, Integer studentId) {
        if (userGroupRepository.existsByGroupIdAndUserId(groupId, studentId)) {
            throw new RuntimeException("Student already in group");
        }

        UserGroup userGroup = new UserGroup();


        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Person student = peopleService.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if(userGroupRepository.findUserGroupByUser(student) != null){
            throw new RuntimeException("Student already in group");
        }

        userGroup.setId(new UserGroupId(studentId, groupId));
        userGroup.setUser(student);
        userGroup.setGroup(group);
        userGroup.setCreatedAt(LocalDateTime.now());

        userGroupRepository.save(userGroup);

        //уведомление: вас добавили в группу
        notificationProducerService.sendGroupAddedNotification(
                studentId,
                student.getRole(),
                group.getName(),
                groupId
        );

    }

    @Transactional
    public void removeStudentFromGroup(Integer groupId, Integer studentId) {
        if (!userGroupRepository.existsByGroupIdAndUserId(groupId, studentId)) {
            throw new RuntimeException("Student not in group");
        }
        userGroupRepository.deleteByGroupIdAndUserId(groupId, studentId);
        Person person = peopleService.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        //уведомление: вас удалили из группы
        notificationProducerService.sendGroupRemovedNotification(
                studentId,
                person.getRole(),
                group.getName(),
                groupId
        );
    }

    @Transactional
    public void removeGroup(Integer groupId) {
        groupRepository.deleteById(groupId);
    }

    public GroupResponseDTO findById(Integer groupId, Authentication auth) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        PersonDetails personDetails = (PersonDetails) auth.getPrincipal();
        Person person = personDetails.getPerson();
       if(!hasAccessToGroup(group, person))
           throw new RuntimeException("Access denied");
        return convertToGroupDTO(group);
    }

    public Integer getUserGroup(String name) {
        Person user = peopleService.findByUsername(name)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserGroup userGroup = userGroupRepository.findUserGroupByUser(user);
        return userGroup.getGroup().getId();
    }

    //метод возвращает юзеров препода (собирает назначенные группы преподу и возвращает их юзеров)
    public List<Person> findByTeacherId(Authentication auth) {
         PersonDetails personDetails = (PersonDetails) auth.getPrincipal();
         Person person = personDetails.getPerson();

        List<Person> students = new ArrayList<>();
        List<Group> groups = groupRepository.findByTeacherId(person.getId());

        for (Group group : groups) {
            List<Person> groupStudents = group.getUserGroups().stream()
                    .map(UserGroup::getUser)
                    .toList();
            students.addAll(groupStudents);
        }

        return students;
    }

    public List<PersonResponseDTO> getStudentsHasGroup(){
        List<UserGroup> studentsId = userGroupRepository.findAll();
        List<Person> students = new ArrayList<>();
        for(UserGroup userGroup : studentsId){
            students.add(userGroup.getUser());
        }
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

    public Integer getUserCountInGroup(Integer groupId) {
        return userGroupRepository.countByGroupId(groupId);
    }
}
