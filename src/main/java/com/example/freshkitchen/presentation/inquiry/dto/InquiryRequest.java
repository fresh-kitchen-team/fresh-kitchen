package com.example.freshkitchen.presentation.inquiry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryRequest(
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 200, message = "제목은 200자 이내여야 합니다")
        String title,

        @NotBlank(message = "내용은 필수입니다")
        @Size(max = 5000, message = "내용은 5000자 이내여야 합니다")
        String content,

        @Email(message = "올바른 이메일 형식이어야 합니다")
        String contactEmail
) {
}
