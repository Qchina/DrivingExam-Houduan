package com.honmen.drivingexam.subject3;

import java.util.List;

public record ExamStep(
    String id,
    ActionType actionType,
    String title,
    String description,
    boolean required,
    int orderIndex,
    int timeWindowSeconds,
    List<VehicleType> vehicleTypes,
    List<RoadScene> roadScenes,
    String correctFeedback,
    String wrongFeedback
) {
    public boolean appliesTo(VehicleType vehicleType, RoadScene roadScene) {
        boolean vehicleMatches = vehicleTypes == null || vehicleTypes.isEmpty() || vehicleTypes.contains(vehicleType);
        boolean sceneMatches = roadScenes == null || roadScenes.isEmpty() || roadScenes.contains(roadScene);
        return vehicleMatches && sceneMatches;
    }
}
