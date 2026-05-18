package com.example.freshkitchen.presentation.analytics;

import com.example.freshkitchen.application.analytics.dto.AnalyticsDto;
import com.example.freshkitchen.application.analytics.usecase.GetAnalyticsSummaryUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final GetAnalyticsSummaryUseCase getAnalyticsSummaryUseCase;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AnalyticsDto.SummaryResponse>> summary(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                getAnalyticsSummaryUseCase.get(new GetAnalyticsSummaryUseCase.Query(userId))
        );
    }
}
