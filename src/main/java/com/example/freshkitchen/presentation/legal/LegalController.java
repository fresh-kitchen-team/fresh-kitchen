package com.example.freshkitchen.presentation.legal;

import com.example.freshkitchen.application.legal.usecase.AgreeTermsUseCase;
import com.example.freshkitchen.application.legal.usecase.GetTermsAgreementUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.infrastructure.legal.LegalProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/legal")
@RequiredArgsConstructor
public class LegalController {

    private final LegalProperties legalProperties;
    private final AgreeTermsUseCase agreeTermsUseCase;
    private final GetTermsAgreementUseCase getTermsAgreementUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<LegalResponse>> legal() {
        return ApiResponse.success(new LegalResponse(
                legalProperties.getTermsUrl(),
                legalProperties.getPrivacyUrl()
        ));
    }

    @PostMapping("/agreement")
    public ResponseEntity<ApiResponse<AgreementResponse>> agreeTerms(
            @AuthenticationPrincipal Long userId
    ) {
        AgreeTermsUseCase.AgreementResult result = agreeTermsUseCase.agree(userId);
        return ApiResponse.success(new AgreementResponse(
                result.termsAgreedAt(),
                result.privacyAgreedAt()
        ));
    }

    @GetMapping("/agreement")
    public ResponseEntity<ApiResponse<AgreementStatusResponse>> getAgreementStatus(
            @AuthenticationPrincipal Long userId
    ) {
        GetTermsAgreementUseCase.AgreementStatus status = getTermsAgreementUseCase.getStatus(userId);
        return ApiResponse.success(new AgreementStatusResponse(
                status.termsAgreed(),
                status.privacyAgreed(),
                status.termsAgreedAt(),
                status.privacyAgreedAt()
        ));
    }

    public record LegalResponse(
            @Schema(description = "이용약관 URL (Notion)", example = "https://www.notion.so/freshkitchen-terms")
            String termsUrl,

            @Schema(description = "개인정보처리방침 URL (Notion)", example = "https://www.notion.so/freshkitchen-privacy")
            String privacyUrl
    ) {
    }

    public record AgreementResponse(
            @Schema(description = "이용약관 동의 시각", example = "2026-05-17T21:30:00+09:00")
            String termsAgreedAt,

            @Schema(description = "개인정보처리방침 동의 시각", example = "2026-05-17T21:30:00+09:00")
            String privacyAgreedAt
    ) {
    }

    public record AgreementStatusResponse(
            @Schema(description = "이용약관 동의 여부", example = "true")
            boolean termsAgreed,

            @Schema(description = "개인정보처리방침 동의 여부", example = "true")
            boolean privacyAgreed,

            @Schema(description = "이용약관 동의 시각 (미동의 시 null)", example = "2026-05-17T21:30:00+09:00")
            String termsAgreedAt,

            @Schema(description = "개인정보처리방침 동의 시각 (미동의 시 null)", example = "2026-05-17T21:30:00+09:00")
            String privacyAgreedAt
    ) {
    }
}
