package com.mykare.appointment_service.Security;

import com.mykare.appointment_service.Entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final String jwtSecret;
    private final long jwtExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-ms}") long jwtExpirationMs
    ) {
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtExpirationMs);

        Map<String, Object> claims = Map.of(
                "userId", user.getId().toString(),
                "role", user.getRole().name(),
                "fullName", user.getFullName()
        );

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    public OffsetDateTime calculateExpiryTime() {
        return OffsetDateTime.now(ZoneOffset.UTC)
                .plusNanos(jwtExpirationMs * 1_000_000);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}