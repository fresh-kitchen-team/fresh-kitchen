package com.example.freshkitchen.domain.inquiry.entity;

import com.example.freshkitchen.application.inquiry.usecase.InquiryCategory;
import com.example.freshkitchen.application.inquiry.usecase.InquiryType;
import com.example.freshkitchen.domain.common.entity.BaseTimeEntity;
import com.example.freshkitchen.domain.inquiry.enums.InquiryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "inquiry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private InquiryType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private InquiryCategory category;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InquiryStatus status;

    @Column(name = "admin_reply", columnDefinition = "TEXT")
    private String adminReply;

    @Column(name = "answered_at")
    private OffsetDateTime answeredAt;

    private Inquiry(Long userId, InquiryType type, InquiryCategory category,
                    String content, String imageUrl) {
        this.userId = requireNonNull(userId, "userId");
        this.type = requireNonNull(type, "type");
        this.category = requireNonNull(category, "category");
        this.content = requireNonBlank(content, "content");
        this.imageUrl = imageUrl;
        this.status = InquiryStatus.PENDING;
    }

    public static Inquiry create(CreateCommand command) {
        return new Inquiry(
                command.userId(),
                command.type(),
                command.category(),
                command.content(),
                command.imageUrl()
        );
    }

    public void answer(String reply) {
        requireNonBlank(reply, "adminReply");
        this.adminReply = reply;
        this.status = InquiryStatus.ANSWERED;
        this.answeredAt = OffsetDateTime.now();
    }

    public record CreateCommand(
            Long userId,
            InquiryType type,
            InquiryCategory category,
            String content,
            String imageUrl
    ) {
    }
}
