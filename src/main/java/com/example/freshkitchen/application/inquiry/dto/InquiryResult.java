package com.example.freshkitchen.application.inquiry.dto;

import com.example.freshkitchen.domain.inquiry.entity.Inquiry;
import com.example.freshkitchen.domain.inquiry.enums.InquiryCategory;
import com.example.freshkitchen.domain.inquiry.enums.InquiryStatus;
import com.example.freshkitchen.domain.inquiry.enums.InquiryType;

import java.time.OffsetDateTime;
import java.util.List;

public record InquiryResult(
        Long id,
        Long userId,
        InquiryType type,
        InquiryCategory category,
        String content,
        String imageUrl,
        InquiryStatus status,
        String adminReply,
        OffsetDateTime answeredAt,
        OffsetDateTime createdAt
) {

    public static InquiryResult from(Inquiry inquiry) {
        return new InquiryResult(
                inquiry.getId(),
                inquiry.getUserId(),
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

    public static List<InquiryResult> from(List<Inquiry> inquiries) {
        return inquiries.stream().map(InquiryResult::from).toList();
    }
}
