package com.honmen.drivingexam.subject3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvaluateTextRequest(
    @NotBlank String projectId,
    @NotNull VehicleType vehicleType,
    String region,
    @NotBlank String userAnswerText
) {
    public String normalizedRegion() {
        return region == null || region.isBlank() ? "default" : region;
    }
}
