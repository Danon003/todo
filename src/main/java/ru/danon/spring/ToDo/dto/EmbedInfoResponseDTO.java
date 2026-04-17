// EmbedInfoResponseDTO.java
package ru.danon.spring.ToDo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbedInfoResponseDTO {
    private VideoMeetingDTO meeting;
    private String embedUrl;
    private boolean isModerator;
    private String userName;
}