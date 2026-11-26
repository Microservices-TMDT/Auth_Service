package com.example.auth_service.service;

import com.example.auth_service.dto.request.LoginRequest;
import com.example.auth_service.dto.request.UserRegistrationDto;
import com.example.auth_service.dto.response.TokenResponse;

public interface UserService {

    void createUser(UserRegistrationDto dto);

    TokenResponse login(LoginRequest request);
}
