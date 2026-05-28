package com.honmen.drivingexam.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Question(
    long id,
    int subject,
    String type,
    String title,
    @JsonProperty("option_a") String optionA,
    @JsonProperty("option_b") String optionB,
    @JsonProperty("option_c") String optionC,
    @JsonProperty("option_d") String optionD,
    String answer,
    String description,
    String image,
    String video
) {
}
