package com.honmen.drivingexam.subject3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record EvaluateActionRequest(
    @NotBlank String projectId,
    @NotBlank String currentStepId,
    @NotNull ActionType actionType,
    @NotNull VehicleType vehicleType,
    @NotNull RoadScene roadScene,
    String region,
    int elapsedSeconds,
    int currentScore,
    List<String> completedStepIds,
    Map<String, Object> context
) {
    public String normalizedRegion() {
        return region == null || region.isBlank() ? "default" : region;
    }
}
