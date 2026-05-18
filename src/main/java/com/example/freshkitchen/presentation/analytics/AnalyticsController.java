package com.example.freshkitchen.presentation.analytics;

import com.example.freshkitchen.application.analytics.dto.AnalyticsDto;
import com.example.freshkitchen.application.analytics.usecase.GetAnalyticsSummaryUseCase;
import com.example.freshkitchen.application.analytics.usecase.ListExpiringItemsUseCase;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final GetAnalyticsSummaryUseCase getAnalyticsSummaryUseCase;
    private final ListExpiringItemsUseCase listExpiringItemsUseCase;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AnalyticsDto.SummaryResponse>> summary(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                getAnalyticsSummaryUseCase.get(new GetAnalyticsSummaryUseCase.Query(userId))
        );
    }

    @GetMapping("/expiring-items")
    public ResponseEntity<ApiResponse<List<AnalyticsDto.ExpiringItem>>> expiringItems(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false)
            @Schema(description = "유통기한 D-Day 필터 (예: 3이면 D-3 이내 항목만 조회)")
            Integer maxDDay,
            @RequestParam(required = false)
            @Schema(description = "저장방법 필터 (FRIDGE, FREEZER, PANTRY)")
            StorageType storageType
    ) {
        return ApiResponse.success(
                listExpiringItemsUseCase.list(
                        new ListExpiringItemsUseCase.Query(userId, maxDDay, storageType)
                )
        );
    }
}
