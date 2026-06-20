package com.mykare.appointment_service.Controller;

import com.mykare.appointment_service.Common.Response.ApiResponse;
import com.mykare.appointment_service.DTO.Request.RegisterRequest;
import com.mykare.appointment_service.DTO.Response.RegisterResponse;
import com.mykare.appointment_service.Service.Interface.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED.value(), "User registered successfully", response));
    }
}
