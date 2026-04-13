package ru.danon.spring.ToDo.dto;

import lombok.Data;

@Data
public class StatisticDTO {

    PersonResponseDTO myInfo;
    int groupCount;
    int peopleCount;
    int tasksCount;
}
