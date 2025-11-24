package ru.danon.spring.ToDo.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.danon.spring.ToDo.models.Person;
import ru.danon.spring.ToDo.models.VideoMeeting;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VideoMeetingRepository extends JpaRepository<VideoMeeting, Integer> {

    List<VideoMeeting> findByCreatedById(Integer createdById);

    List<VideoMeeting> findByGroupId(Integer groupId);

    @Query("SELECT vm FROM VideoMeeting vm WHERE vm.group.id = :groupId AND vm.isActive = true")
    List<VideoMeeting> findActiveByGroupId(@Param("groupId") Integer groupId);

    @Query("SELECT vm FROM VideoMeeting vm WHERE vm.createdBy.id = :userId AND vm.isActive = true")
    List<VideoMeeting> findActiveByCreatedById(@Param("userId") Integer userId);

    /**
     * Находит активные встречи для студента (его группы или без группы)
     */
    @Query("SELECT vm FROM VideoMeeting vm WHERE vm.isActive = true " +
            "AND (vm.group IS NULL OR vm.group.id = :studentGroupId) " +
            "ORDER BY vm.startTime DESC")
    List<VideoMeeting> findActiveForStudent(@Param("studentGroupId") Integer studentGroupId);

    /**
     * Находит активные встречи без привязки к группе
     */
    @Query("SELECT vm FROM VideoMeeting vm WHERE vm.isActive = true AND vm.group IS NULL " +
            "ORDER BY vm.startTime DESC")
    List<VideoMeeting> findActiveWithoutGroup();

    /**
     * Находит встречи созданные пользователем
     */
    List<VideoMeeting> findByCreatedByAndIsActive(Person createdBy, Boolean isActive);

    List<VideoMeeting> findByIsActiveTrueAndGroupIdOrGroupIsNull(Integer studentGroupId);

    @Query("SELECT vm FROM VideoMeeting vm WHERE vm.isActive = true AND " +
            "((vm.endTime IS NOT NULL AND vm.endTime < :threshold) OR " +
            "(vm.endTime IS NULL AND vm.startTime < :threshold))")
    List<VideoMeeting> findMeetingsEndedBefore(@Param("threshold") LocalDateTime threshold);

    List<VideoMeeting> findByIsActiveTrueAndReminderSentFalseAndStartTimeBetween(
            LocalDateTime start,
            LocalDateTime end
    );

}




