package com.honmen.drivingexam.controller;

import com.honmen.drivingexam.dto.AiDtos.AiChatRequest;
import com.honmen.drivingexam.dto.AiDtos.AiChatResponse;
import com.honmen.drivingexam.dto.ApiResponse;
import com.honmen.drivingexam.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final AiChatService service;

    public AiController(AiChatService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return ApiResponse.success(service.chat(request));
    }
}
