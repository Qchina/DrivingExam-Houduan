package com.honmen.drivingexam.controller;

import com.honmen.drivingexam.dto.ApiResponse;
import com.honmen.drivingexam.dto.BusinessDtos.FavoriteQuestion;
import com.honmen.drivingexam.dto.BusinessDtos.FavoriteRequest;
import com.honmen.drivingexam.dto.PageResult;
import com.honmen.drivingexam.service.DrivingExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {
    private final DrivingExamService service;

    public FavoriteController(DrivingExamService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<FavoriteQuestion>> list(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam int subject,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int limit
    ) {
        long userId = service.requireUserId(authorization);
        return ApiResponse.success(service.listFavorites(userId, subject, page, limit));
    }

    @PostMapping
    public ApiResponse<FavoriteQuestion> add(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody FavoriteRequest request
    ) {
        long userId = service.requireUserId(authorization);
        return ApiResponse.success(service.addFavorite(userId, request.questionId(), request.subject()));
    }

    @DeleteMapping("/{questionId}")
    public ApiResponse<Void> remove(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable long questionId
    ) {
        long userId = service.requireUserId(authorization);
        service.removeFavorite(userId, questionId);
        return ApiResponse.success(null);
    }
}
