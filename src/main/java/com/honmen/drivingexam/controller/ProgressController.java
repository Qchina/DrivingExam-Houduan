package com.honmen.drivingexam.controller;

import com.honmen.drivingexam.dto.ApiResponse;
import com.honmen.drivingexam.dto.BusinessDtos.PracticeQuestionStatus;
import com.honmen.drivingexam.dto.BusinessDtos.ProgressRequest;
import com.honmen.drivingexam.model.PracticeProgress;
import com.honmen.drivingexam.service.DrivingExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/progress")
public class ProgressController {
    private final DrivingExamService service;

    public ProgressController(DrivingExamService service) {
        this.service = service;
    }

    @PutMapping
    public ApiResponse<PracticeProgress> sync(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody ProgressRequest request
    ) {
        long userId = service.requireUserId(authorization);
        return ApiResponse.success(service.syncProgress(userId, request));
    }

    @GetMapping("/status")
    public ApiResponse<List<PracticeQuestionStatus>> status(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam int subject,
        @RequestParam(required = false) String questionIds
    ) {
        long userId = service.requireUserId(authorization);
        return ApiResponse.success(service.listPracticeStatuses(userId, subject, questionIds));
    }
}
