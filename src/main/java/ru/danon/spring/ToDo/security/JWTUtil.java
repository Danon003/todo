package ru.danon.spring.ToDo.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Date;

@Component
public class JWTUtil {

    @Value("${jwt_secret}")
    private String secret;

    public String generateToken(String username) {
        Date expirationDate = Date.from(ZonedDateTime.now().plusMinutes(60).toInstant());
        return JWT.create()
                .withSubject("User details")
                .withClaim("username", username)//параметры, которые передаются в токен
                .withIssuedAt(new Date())//время когда создан
                .withIssuer("Danon")//кто выдал токен
                .withExpiresAt(expirationDate)//когда заканчивается срок действия
                .sign(Algorithm.HMAC256(secret));//секрет

    }

    public String validateTokenAndRetrieveClaim(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                .withSubject("User details")
                .withIssuer("Danon")
                .build();

        DecodedJWT jwt = verifier.verify(token);

        return jwt.getClaim("username").asString();
    }
}
