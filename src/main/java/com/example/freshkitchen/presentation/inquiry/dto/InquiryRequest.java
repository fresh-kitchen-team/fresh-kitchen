package com.example.freshkitchen.presentation.inquiry.dto;

import com.example.freshkitchen.application.inquiry.usecase.InquiryCategory;
import com.example.freshkitchen.application.inquiry.usecase.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InquiryRequest(
        @NotNull(message = "문의 유형은 필수입니다")
        @Schema(description = "문의 유형 (INQUIRY: 문의, REPORT: 신고)", example = "INQUIRY")
        InquiryType type,

        @NotNull(message = "문의 카테고리는 필수입니다")
        @Schema(description = "카테고리 (RECIPE: 레시피 관련, AI: AI 관련, OTHER: 기타)", example = "RECIPE")
        InquiryCategory category,

        @NotBlank(message = "내용은 필수입니다")
        @Size(max = 5000, message = "내용은 5000자 이내여야 합니다")
        @Schema(description = "문의/신고 내용", example = "레시피 추천이 잘못된 것 같습니다.")
        String content
) {
}
