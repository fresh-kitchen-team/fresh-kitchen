package com.example.freshkitchen.presentation.inquiry.dto;

import com.example.freshkitchen.application.inquiry.dto.InquiryResult;
import com.example.freshkitchen.domain.inquiry.enums.InquiryCategory;
import com.example.freshkitchen.domain.inquiry.enums.InquiryStatus;
import com.example.freshkitchen.domain.inquiry.enums.InquiryType;
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
        public static Summary from(InquiryResult result) {
            String preview = result.content().length() > 50
                    ? result.content().substring(0, 50) + "…"
                    : result.content();
            return new Summary(
                    result.id(),
                    result.type(),
                    result.category(),
                    preview,
                    result.status(),
                    result.createdAt()
            );
        }

        public static List<Summary> from(List<InquiryResult> results) {
            return results.stream().map(Summary::from).toList();
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
        public static Detail from(InquiryResult result) {
            return new Detail(
                    result.id(),
                    result.type(),
                    result.category(),
                    result.content(),
                    result.imageUrl(),
                    result.status(),
                    result.adminReply(),
                    result.answeredAt(),
                    result.createdAt()
            );
        }
    }

    @Schema(description = "문의 생성 결과")
    public record Created(
            @Schema(description = "생성된 문의 ID") Long inquiryId
    ) {
    }
}
