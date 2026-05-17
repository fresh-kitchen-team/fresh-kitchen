package com.example.freshkitchen.presentation.inquiry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryRequest(
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 200, message = "제목은 200자 이내여야 합니다")
        @Schema(description = "문의 제목", example = "앱 사용 중 버그 발생")
        String title,

        @NotBlank(message = "내용은 필수입니다")
        @Size(max = 5000, message = "내용은 5000자 이내여야 합니다")
        @Schema(description = "문의 내용", example = "재료 추가 버튼을 클릭하면 앱이 강제 종료됩니다. 안드로이드 버전 12에서 발생합니다.")
        String content,

        @Email(message = "올바른 이메일 형식이어야 합니다")
        @Schema(description = "연락 이메일 (선택)", example = "user@example.com")
        String contactEmail
) {
}
