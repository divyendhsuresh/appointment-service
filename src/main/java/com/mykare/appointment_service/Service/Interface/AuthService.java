package com.mykare.appointment_service.Service.Interface;

import com.mykare.appointment_service.DTO.Request.LoginRequest;
import com.mykare.appointment_service.DTO.Request.RegisterRequest;
import com.mykare.appointment_service.DTO.Response.LoginResponse;
import com.mykare.appointment_service.DTO.Response.LogoutResponse;
import com.mykare.appointment_service.DTO.Response.RegisterResponse;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    LogoutResponse logout(String token);
}
