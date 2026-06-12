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
        String context,
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

    public record AiDailyTaskRequest(
        Long userId,
        Integer subject,
        String context,
        Boolean forceRefresh,
        Boolean cacheOnly
    ) {
    }

    public record AiDailyTask(
        String title,
        String subtitle,
        String actionType
    ) {
    }

    public record AiDailyTaskResponse(
        String summary,
        List<AiDailyTask> tasks,
        String model,
        String mode,
        Boolean cached,
        LocalDateTime createdAt
    ) {
    }
}
