package com.mykare.appointment_service.Security;

import com.mykare.appointment_service.Entity.User;
import io.jsonwebtoken.Claims;
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
import java.util.UUID;
import java.util.function.Function;

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

    /**
     * Generates a signed JWT token for the authenticated user.
     */
    public String generateToken(User user) {

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(jwtExpirationMs);

        Map<String, Object> claims = Map.of(
                "userId", user.getId().toString(),
                "role", user.getRole().name(),
                "fullName", user.getFullName()
        );

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the email stored as the JWT subject.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public String extractUserId(String token) {
        return extractClaim(
                token,
                claims -> claims.get("userId", String.class)
        );
    }

    /**
     * Extracts the user's role from the JWT.
     */
    public String extractRole(String token) {
        return extractClaim(
                token,
                claims -> claims.get("role", String.class)
        );
    }

    /**
     * Extracts the token expiry date.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenValid(
            String token,
            String email
    ) {

        String tokenEmail = extractEmail(token);

        return tokenEmail != null
                && tokenEmail.equalsIgnoreCase(email)
                && !isTokenExpired(token);
    }

    /**
     * Returns true when the JWT expiry date is before now.
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Returns the expiry timestamp for the login response.
     */
    public OffsetDateTime calculateExpiryTime() {
        return Instant.now()
                .plusMillis(jwtExpirationMs)
                .atOffset(ZoneOffset.UTC);
    }

    /**
     * Extracts an individual claim using the supplied resolver.
     */
    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimResolver
    ) {

        Claims claims = extractAllClaims(token);

        return claimResolver.apply(claims);
    }

    /**
     * Parses and verifies the JWT signature.
     *
     * This method throws a JwtException when:
     * - the signature is invalid
     * - the token is expired
     * - the token format is invalid
     */
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Creates the HMAC signing key from the Base64 secret.
     */
    private SecretKey getSigningKey() {

        byte[] keyBytes =
                Decoders.BASE64.decode(jwtSecret);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}