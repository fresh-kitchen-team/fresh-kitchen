package com.example.freshkitchen.presentation.inquiry.dto;

import com.example.freshkitchen.domain.inquiry.enums.InquiryCategory;
import com.example.freshkitchen.domain.inquiry.enums.InquiryType;
import com.example.freshkitchen.domain.inquiry.entity.Inquiry;
import com.example.freshkitchen.domain.inquiry.enums.InquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

public final class InquiryResponse {

    private InquiryResponse() {
    }

    @Schema(description = "문의 목록 항목")
    public record Summary(
            @Schema(description = "문의 ID") Long id,
            @Schema(description = "문의 유형") InquiryType type,
            @Schema(description = "카테고리") InquiryCategory category,
            @Schema(description = "내용 미리보기 (50자)") String contentPreview,
            @Schema(description = "상태") InquiryStatus status,
            @Schema(description = "작성일") OffsetDateTime createdAt
    ) {
        public static Summary from(Inquiry inquiry) {
            String preview = inquiry.getContent().length() > 50
                    ? inquiry.getContent().substring(0, 50) + "…"
                    : inquiry.getContent();
            return new Summary(
                    inquiry.getId(),
                    inquiry.getType(),
                    inquiry.getCategory(),
                    preview,
                    inquiry.getStatus(),
                    inquiry.getCreatedAt()
            );
        }

        public static List<Summary> from(List<Inquiry> inquiries) {
            return inquiries.stream().map(Summary::from).toList();
        }
    }

    @Schema(description = "문의 상세")
    public record Detail(
            @Schema(description = "문의 ID") Long id,
            @Schema(description = "문의 유형") InquiryType type,
            @Schema(description = "카테고리") InquiryCategory category,
            @Schema(description = "내용") String content,
            @Schema(description = "첨부 이미지 URL") String imageUrl,
            @Schema(description = "상태") InquiryStatus status,
            @Schema(description = "관리자 답변") String adminReply,
            @Schema(description = "답변 시각") OffsetDateTime answeredAt,
            @Schema(description = "작성일") OffsetDateTime createdAt
    ) {
        public static Detail from(Inquiry inquiry) {
            return new Detail(
                    inquiry.getId(),
                    inquiry.getType(),
                    inquiry.getCategory(),
                    inquiry.getContent(),
                    inquiry.getImageUrl(),
                    inquiry.getStatus(),
                    inquiry.getAdminReply(),
                    inquiry.getAnsweredAt(),
                    inquiry.getCreatedAt()
            );
        }
    }

    @Schema(description = "문의 생성 결과")
    public record Created(
            @Schema(description = "생성된 문의 ID") Long inquiryId
    ) {
    }
}
