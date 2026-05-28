package com.honmen.drivingexam.model;

public record Question(
    long id,
    int subject,
    String type,
    String title,
    String optionA,
    String optionB,
    String optionC,
    String optionD,
    String answer,
    String description,
    String image,
    String video
) {
}
