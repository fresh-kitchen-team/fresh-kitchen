package com.example.freshkitchen.presentation.tips;

import com.example.freshkitchen.application.analytics.dto.DisplayCategory;
import com.example.freshkitchen.application.tips.usecase.GetRecyclingGuidesUseCase;
import com.example.freshkitchen.application.tips.usecase.GetStorageTipsUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tips")
@RequiredArgsConstructor
public class TipsController {

    private final GetStorageTipsUseCase getStorageTipsUseCase;
    private final GetRecyclingGuidesUseCase getRecyclingGuidesUseCase;

    @GetMapping("/storage")
    public ResponseEntity<ApiResponse<List<GetStorageTipsUseCase.StorageTipResult>>> storageTips(
            @RequestParam(required = false)
            @Schema(description = "카테고리 필터 (VEGETABLE_FRUIT, DAIRY_DRINK, MEAT_SEAFOOD, ETC)")
            DisplayCategory category
    ) {
        return ApiResponse.success(
                getStorageTipsUseCase.get(new GetStorageTipsUseCase.Query(category))
        );
    }

    @GetMapping("/recycling")
    public ResponseEntity<ApiResponse<List<GetRecyclingGuidesUseCase.RecyclingGuideResult>>> recyclingGuides() {
        return ApiResponse.success(
                getRecyclingGuidesUseCase.get()
        );
    }
}
