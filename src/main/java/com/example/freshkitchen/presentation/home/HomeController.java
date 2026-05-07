package com.example.freshkitchen.presentation.home;

import com.example.freshkitchen.application.home.dto.HomeDto;
import com.example.freshkitchen.application.home.usecase.GetHomeSummaryUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final GetHomeSummaryUseCase getHomeSummaryUseCase;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<HomeDto.SummaryResponse>> summary(
            @RequestHeader(USER_ID_HEADER) @Positive Long userId
    ) {
        return ApiResponse.success(
                getHomeSummaryUseCase.get(new GetHomeSummaryUseCase.Query(userId))
        );
    }
}
