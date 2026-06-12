package com.honmen.drivingexam.controller;

import com.honmen.drivingexam.dto.AiDtos.AiChatRequest;
import com.honmen.drivingexam.dto.AiDtos.AiChatResponse;
import com.honmen.drivingexam.dto.AiDtos.AiDailyTaskRequest;
import com.honmen.drivingexam.dto.AiDtos.AiDailyTaskResponse;
import com.honmen.drivingexam.dto.ApiResponse;
import com.honmen.drivingexam.service.AiChatService;
import com.honmen.drivingexam.service.DrivingExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final AiChatService service;
    private final DrivingExamService drivingExamService;

    public AiController(AiChatService service, DrivingExamService drivingExamService) {
        this.service = service;
        this.drivingExamService = drivingExamService;
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return ApiResponse.success(service.chat(request));
    }

    @PostMapping("/daily-tasks")
    public ApiResponse<AiDailyTaskResponse> dailyTasks(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestBody AiDailyTaskRequest request
    ) {
        AiDailyTaskRequest resolvedRequest = request;
        if (request != null && (request.userId() == null || request.userId() <= 0) && authorization != null && !authorization.isBlank()) {
            resolvedRequest = new AiDailyTaskRequest(
                drivingExamService.requireUserId(authorization),
                request.subject(),
                request.context(),
                request.forceRefresh(),
                request.cacheOnly()
            );
        }
        return ApiResponse.success(service.dailyTasks(resolvedRequest));
    }
}
