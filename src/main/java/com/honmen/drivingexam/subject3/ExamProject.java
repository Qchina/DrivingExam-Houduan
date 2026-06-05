package com.honmen.drivingexam.subject3;

import java.util.List;

public record ExamProject(
    String id,
    String name,
    String description,
    String region,
    List<VehicleType> vehicleTypes,
    List<RoadScene> roadScenes,
    List<ExamStep> steps,
    List<ScoreRule> scoreRules,
    List<ScoreRule> failRules,
    List<String> tips
) {
    public boolean appliesTo(VehicleType vehicleType, String requestedRegion) {
        boolean vehicleMatches = vehicleTypes == null || vehicleTypes.isEmpty() || vehicleTypes.contains(vehicleType);
        boolean regionMatches = region.equals("default") || region.equals(requestedRegion);
        return vehicleMatches && regionMatches;
    }
}
