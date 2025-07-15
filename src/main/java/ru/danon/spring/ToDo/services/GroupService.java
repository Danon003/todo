package ru.danon.spring.ToDo.services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.time.LocalDateTime;
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
    private final ModelMapper modelMapper;

    @Autowired
    public GroupService(GroupRepository groupRepository, UserGroupRepository userGroupRepository, PeopleService peopleService, ModelMapper modelMapper) {
        this.groupRepository = groupRepository;
        this.userGroupRepository = userGroupRepository;
        this.peopleService = peopleService;
        this.modelMapper = modelMapper;
    }

    public List<GroupResponseDTO> findAll() {
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


        System.out.println("Ищем группу с ID: {}"+ groupId);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Person student = peopleService.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        UserGroup userGroup = new UserGroup();
        userGroup.setId(new UserGroupId(studentId, groupId));
        userGroup.setUser(student);
        userGroup.setGroup(group);
        userGroup.setCreatedAt(LocalDateTime.now());

        userGroupRepository.save(userGroup);
    }

    @Transactional
    public void removeStudentFromGroup(Integer groupId, Integer studentId) {
        if (!userGroupRepository.existsByGroupIdAndUserId(groupId, studentId)) {
            throw new RuntimeException("Student not in group");
        }
        userGroupRepository.deleteByGroupIdAndUserId(groupId, studentId);
    }

    @Transactional
    public void removeGroup(Integer groupId) {
        groupRepository.deleteById(groupId);
    }

    private List<PersonResponseDTO> convertToResponsePerson(List<Person> allUsers) {
        return allUsers.stream()
                .map(user -> modelMapper.map(user, PersonResponseDTO.class))
                .collect(Collectors.toList());
    }

    public GroupResponseDTO findById(Integer groupId) {
        return convertToGroupDTO(groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found")));
    }

    public Integer getUserGroup(String name) {
        Person user = peopleService.findByUsername(name)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserGroup userGroup = userGroupRepository.findUserGroupByUser(user);
        return userGroup.getGroup().getId();
    }

    private GroupResponseDTO convertToGroupDTO(Group group) {
        return modelMapper.map(group, GroupResponseDTO.class);
    }
}
