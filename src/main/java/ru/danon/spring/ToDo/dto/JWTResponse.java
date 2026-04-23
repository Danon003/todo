package ru.danon.spring.ToDo.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class JWTResponse {
    @JsonProperty("jwt-token")
    private String jwtToken;
}