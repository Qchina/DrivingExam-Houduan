package com.honmen.drivingexam.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record AuthResponse(String token, long userId, String nickname) {
    }

    public record UpdateUsernameRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record UserProfileResponse(long userId, String username, String nickname) {
    }
}
