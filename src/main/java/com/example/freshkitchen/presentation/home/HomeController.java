package com.example.freshkitchen.presentation.home;

import com.example.freshkitchen.application.home.dto.HomeDto;
import com.example.freshkitchen.application.home.usecase.GetHomeSummaryUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final GetHomeSummaryUseCase getHomeSummaryUseCase;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<HomeDto.SummaryResponse>> summary(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                getHomeSummaryUseCase.get(new GetHomeSummaryUseCase.Query(userId))
        );
    }
}
