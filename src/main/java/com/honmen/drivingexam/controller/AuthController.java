package com.honmen.drivingexam.controller;

import com.honmen.drivingexam.dto.ApiResponse;
import com.honmen.drivingexam.dto.AuthDtos.AuthResponse;
import com.honmen.drivingexam.dto.AuthDtos.LoginRequest;
import com.honmen.drivingexam.dto.AuthDtos.UpdatePasswordRequest;
import com.honmen.drivingexam.dto.AuthDtos.UpdateUsernameRequest;
import com.honmen.drivingexam.dto.AuthDtos.UserProfileResponse;
import com.honmen.drivingexam.service.DrivingExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final DrivingExamService service;

    public AuthController(DrivingExamService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(service.login(request.username(), request.password()));
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(service.register(request.username(), request.password()));
    }

    @PutMapping("/username")
    public ApiResponse<UserProfileResponse> updateUsername(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody UpdateUsernameRequest request
    ) {
        return ApiResponse.success(service.updateUsername(authorization, request.username(), request.password()));
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody UpdatePasswordRequest request
    ) {
        service.updatePassword(authorization, request.password(), request.newPassword());
        return ApiResponse.success(null);
    }
}
