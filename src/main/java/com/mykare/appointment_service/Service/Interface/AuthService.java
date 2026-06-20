package com.mykare.appointment_service.Service.Interface;

import com.mykare.appointment_service.DTO.Request.RegisterRequest;
import com.mykare.appointment_service.DTO.Response.RegisterResponse;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
}
