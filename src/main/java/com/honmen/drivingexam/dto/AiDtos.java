package com.honmen.drivingexam.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

public final class AiDtos {
    private AiDtos() {
    }

    public record ChatMessage(String role, String content) {
    }

    public record AiChatRequest(
        @NotBlank String message,
        Long userId,
        Integer subject,
        String scene,
        List<ChatMessage> history
    ) {
    }

    public record AiChatResponse(
        String reply,
        String model,
        String mode,
        LocalDateTime createdAt
    ) {
    }
}
