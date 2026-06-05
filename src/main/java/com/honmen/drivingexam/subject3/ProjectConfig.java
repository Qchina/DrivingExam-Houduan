package com.honmen.drivingexam.subject3;

import java.util.List;

public record ProjectConfig(
    String id,
    String name,
    String description,
    String voiceText,
    List<VehicleType> vehicleTypes,
    List<String> keywords,
    List<StandardStep> standardSteps,
    List<String> commonMistakes,
    String region,
    List<String> tips,
    List<RoadScene> roadScenes,
    List<ExamStep> steps,
    List<ScoreRule> scoreRules,
    List<ScoreRule> failRules
) {
}
