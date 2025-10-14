package ru.danon.spring.ToDo.dto;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.danon.spring.ToDo.models.Person;

import java.time.LocalDateTime;


public class LogResponseDTO {

        private Integer id;
        private PersonDTO user;
        private String oldRole;
        private String newRole;
        private LocalDateTime changedAt;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public PersonDTO getUser() {
            return user;
        }

        public void setUser(PersonDTO user) {
            this.user = user;
        }

        public String getOldRole() {
            return oldRole;
        }

        public void setOldRole(String oldRole) {
            this.oldRole = oldRole;
        }

        public String getNewRole() {
            return newRole;
        }

        public void setNewRole(String newRole) {
            this.newRole = newRole;
        }

        public LocalDateTime getChangedAt() {
            return changedAt;
        }

        public void setChangedAt(LocalDateTime changedAt) {
            this.changedAt = changedAt;
        }

}
