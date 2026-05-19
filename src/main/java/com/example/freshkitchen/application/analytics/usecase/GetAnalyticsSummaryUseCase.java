package com.example.freshkitchen.application.analytics.usecase;

import com.example.freshkitchen.application.analytics.dto.AnalyticsDto;

public interface GetAnalyticsSummaryUseCase {

    AnalyticsDto.SummaryResponse get(Query query);

    record Query(Long userId) {
    }
}
