package com.honmen.drivingexam.controller;

import com.honmen.drivingexam.dto.ApiResponse;
import com.honmen.drivingexam.dto.BusinessDtos.ErrorQuestion;
import com.honmen.drivingexam.dto.BusinessDtos.ErrorReviewRequest;
import com.honmen.drivingexam.dto.BusinessDtos.ErrorRequest;
import com.honmen.drivingexam.dto.PageResult;
import com.honmen.drivingexam.service.DrivingExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/errors")
public class ErrorController {
    private final DrivingExamService service;

    public ErrorController(DrivingExamService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<ErrorQuestion>> list(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam int subject,
        @RequestParam(required = false) Integer isMastered,
        @RequestParam(defaultValue = "all") String scope,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int limit
    ) {
        long userId = service.requireUserId(authorization);
        return ApiResponse.success(service.listErrors(userId, subject, isMastered, scope, page, limit));
    }

    @PostMapping
    public ApiResponse<ErrorQuestion> record(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody ErrorRequest request
    ) {
        long userId = service.requireUserId(authorization);
        return ApiResponse.success(service.recordError(userId, request.questionId(), request.subject(), request.wrongAnswer()));
    }

    @PutMapping("/{questionId}/mastered")
    public ApiResponse<ErrorQuestion> mastered(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable long questionId
    ) {
        long userId = service.requireUserId(authorization);
        return ApiResponse.success(service.markErrorMastered(userId, questionId));
    }

    @PostMapping("/review-result")
    public ApiResponse<ErrorQuestion> reviewResult(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody ErrorReviewRequest request
    ) {
        long userId = service.requireUserId(authorization);
        return ApiResponse.success(
            service.reviewErrorQuestion(
                userId,
                request.questionId(),
                request.subject(),
                request.isCorrect(),
                request.removeThreshold()
            )
        );
    }
}
