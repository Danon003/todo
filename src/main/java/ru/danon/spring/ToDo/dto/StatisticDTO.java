package ru.danon.spring.ToDo.dto;

public class StatisticDTO {

    PersonResponseDTO myInfo;
    int groupCount;
    int peopleCount;
    int tasksCount;

    public StatisticDTO() {}

    public StatisticDTO(PersonResponseDTO myInfo, int groupCount, int peopleCount, int tasksCount) {
        this.myInfo = myInfo;
        this.groupCount = groupCount;
        this.peopleCount = peopleCount;
        this.tasksCount = tasksCount;
    }

    public PersonResponseDTO getMyInfo() {
        return myInfo;
    }

    public void setMyInfo(PersonResponseDTO myInfo) {
        this.myInfo = myInfo;
    }

    public int getGroupCount() {
        return groupCount;
    }

    public void setGroupCount(int groupCount) {
        this.groupCount = groupCount;
    }

    public int getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(int peopleCount) {
        this.peopleCount = peopleCount;
    }

    public int getTasksCount() {
        return tasksCount;
    }

    public void setTasksCount(int tasksCount) {
        this.tasksCount = tasksCount;
    }
}
