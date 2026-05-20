package com.example.freshkitchen.application.tips.usecase;

import java.util.List;

public interface GetRecyclingGuidesUseCase {

    List<RecyclingGuideResult> get();

    record RecyclingGuideResult(
            Long id,
            String name,
            String wasteType,
            String description
    ) {
    }
}
