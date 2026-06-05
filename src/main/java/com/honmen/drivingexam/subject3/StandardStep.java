package com.honmen.drivingexam.subject3;

import java.util.List;

public record StandardStep(
    String id,
    String title,
    List<String> aliases,
    boolean required,
    int order,
    int score,
    boolean failIfMissing,
    List<VehicleType> vehicleTypes
) {
}
