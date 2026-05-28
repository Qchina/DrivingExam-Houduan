package com.honmen.drivingexam.controller;

import com.honmen.drivingexam.dto.ApiResponse;
import com.honmen.drivingexam.dto.BusinessDtos.ExamSubmitRequest;
import com.honmen.drivingexam.model.ExamHistory;
import com.honmen.drivingexam.model.Question;
import com.honmen.drivingexam.service.DrivingExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exams")
public class ExamController {
    private final DrivingExamService service;

    public ExamController(DrivingExamService service) {
        this.service = service;
    }

    @GetMapping("/paper")
    public ApiResponse<List<Question>> paper(@RequestParam int subject) {
        return ApiResponse.success(service.examPaper(subject));
    }

    @PostMapping("/submit")
    public ApiResponse<ExamHistory> submit(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody ExamSubmitRequest request
    ) {
        long userId = service.requireUserId(authorization);
        return ApiResponse.success(service.submitExam(userId, request));
    }

    @GetMapping("/history")
    public ApiResponse<List<ExamHistory>> history(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam int subject
    ) {
        long userId = service.requireUserId(authorization);
        return ApiResponse.success(service.examHistory(userId, subject));
    }
}
