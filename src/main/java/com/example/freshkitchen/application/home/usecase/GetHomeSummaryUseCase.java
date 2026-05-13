package com.example.freshkitchen.application.home.usecase;

import com.example.freshkitchen.application.home.dto.HomeDto;

public interface GetHomeSummaryUseCase {

    HomeDto.SummaryResponse get(Query query);

    record Query(Long userId) {
    }
}
