package com.publication_trend_tracking_system.sever_web_app.security;

import com.publication_trend_tracking_system.sever_web_app.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    // Read from configuration so the deployed secret can be supplied through the environment
    // (JWT_SECRET) and never has to live in the repository. The default below only exists so a
    // fresh clone starts without setup — anyone holding it can mint a token for any account,
    // including an admin one, so a real deployment must override it.
    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration-hours:24}")
    private long expirationHours;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(
                        "role",
                        user.getRole().getRoleName()
                )
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + expirationHours * 60 * 60 * 1000
                        )
                )
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(
                token,
                Claims::getSubject
        );
    }

    public <T> T extractClaim(
            String token,
            Function<Claims, T> resolver) {

        Claims claims = extractAllClaims(token);

        return resolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(
            String token,
            String email) {

        String username =
                extractUsername(token);

        return username.equals(email)
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(
            String token) {

        return extractClaim(
                token,
                Claims::getExpiration
        ).before(new Date());
    }
}