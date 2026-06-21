package com.mykare.appointment_service.Service.Implementation;


import com.mykare.appointment_service.DTO.Request.LoginRequest;
import com.mykare.appointment_service.DTO.Request.RegisterRequest;
import com.mykare.appointment_service.DTO.Response.LoginResponse;
import com.mykare.appointment_service.DTO.Response.LogoutResponse;
import com.mykare.appointment_service.DTO.Response.RegisterResponse;
import com.mykare.appointment_service.Entity.User;
import com.mykare.appointment_service.Enums.UserRole;
import com.mykare.appointment_service.Exception.EmailAlreadyExistsException;
import com.mykare.appointment_service.Exception.InvalidCredentialsException;
import com.mykare.appointment_service.Exception.UserInactiveException;
import com.mykare.appointment_service.Repository.UserRepository;
import com.mykare.appointment_service.Security.JwtService;
import com.mykare.appointment_service.Service.Interface.AuthService;
import com.mykare.appointment_service.Service.Interface.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String normalizedEmail = request.email()
                .trim()
                .toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(
                    "Email is already registered"
            );
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(normalizedEmail)
                .passwordHash(
                        passwordEncoder.encode(request.password())
                )
                .phone(normalizePhone(request.phone()))
                .role(UserRole.USER)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                savedUser.getRole(),
                savedUser.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email()
                .trim()
                .toLowerCase();
        User user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.isActive()) {
            throw new UserInactiveException("User account is inactive");}

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash());

        if (!passwordMatches) {
            throw new InvalidCredentialsException(
                    "Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user);
        return new LoginResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                accessToken,
                "Bearer",
                jwtService.calculateExpiryTime()
        );

    }

    @Override
    public LogoutResponse logout(String token) {

        String tokenId =
                jwtService.extractTokenId(token);

        String email =
                jwtService.extractEmail(token);

        Date expiration =
                jwtService.extractExpiration(token);

        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT token does not contain a token ID"
            );
        }

        tokenBlacklistService.blacklistToken(tokenId, expiration);

        return new LogoutResponse(email, OffsetDateTime.now(ZoneOffset.UTC));
    }
    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        return phone.trim();
    }
}
