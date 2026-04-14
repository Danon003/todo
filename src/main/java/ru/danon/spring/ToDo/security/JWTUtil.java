package ru.danon.spring.ToDo.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Date;

@Component
@Slf4j
public class JWTUtil {

    @Value("${jwt_secret}")
    private String secret;

    public String generateToken(String username) {
        Date expirationDate = Date.from(ZonedDateTime.now().plusMinutes(60).toInstant());
        log.debug("Генерация JWT токена для пользователя: {}", username);

        String token = JWT.create()
                .withSubject("User details")
                .withClaim("username", username)//параметры, которые передаются в токен
                .withIssuedAt(new Date())//время когда создан
                .withIssuer("Danon")//кто выдал токен
                .withExpiresAt(expirationDate)//когда заканчивается срок действия
                .sign(Algorithm.HMAC256(secret));//секрет

        log.debug("JWT токен успешно сгенерирован для пользователя: {}, истекает: {}", username, expirationDate);
        return token;
    }

    public String validateTokenAndRetrieveClaim(String token) throws JWTVerificationException {
        log.debug("Валидация JWT токена");

        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                    .withSubject("User details")
                    .withIssuer("Danon")
                    .build();

            DecodedJWT jwt = verifier.verify(token);
            String username = jwt.getClaim("username").asString();
            log.debug("JWT токен успешно валидирован для пользователя: {}", username);

            return username;
        } catch (JWTVerificationException e) {
            log.warn("Ошибка валидации JWT токена: {}", e.getMessage());
            throw e;
        }
    }
}