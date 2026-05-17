package com.example.freshkitchen.presentation.legal;

import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.infrastructure.legal.LegalProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/legal")
@RequiredArgsConstructor
public class LegalController {

    private final LegalProperties legalProperties;

    @GetMapping
    public ResponseEntity<ApiResponse<LegalResponse>> legal() {
        return ApiResponse.success(new LegalResponse(
                legalProperties.getTermsUrl(),
                legalProperties.getPrivacyUrl()
        ));
    }

    public record LegalResponse(
            @Schema(description = "이용약관 URL (Notion)", example = "https://www.notion.so/freshkitchen-terms")
            String termsUrl,

            @Schema(description = "개인정보처리방침 URL (Notion)", example = "https://www.notion.so/freshkitchen-privacy")
            String privacyUrl
    ) {
    }
}
