package com.example.freshkitchen.application.inquiry.service;

import com.example.freshkitchen.application.inquiry.exception.InquiryErrorCode;
import com.example.freshkitchen.application.inquiry.exception.InquiryException;
import com.example.freshkitchen.application.inquiry.usecase.ReplyInquiryUseCase;
import com.example.freshkitchen.domain.inquiry.entity.Inquiry;
import com.example.freshkitchen.domain.inquiry.enums.InquiryStatus;
import com.example.freshkitchen.domain.inquiry.repository.InquiryRepository;
import com.example.freshkitchen.domain.user.entity.UserFcmToken;
import com.example.freshkitchen.domain.user.repository.UserFcmTokenRepository;
import com.example.freshkitchen.infrastructure.fcm.FcmMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplyInquiryService implements ReplyInquiryUseCase {

    private final InquiryRepository inquiryRepository;
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final FcmMessageSender fcmMessageSender;

    @Override
    @Transactional
    public void reply(Command command) {
        // 비관적 잠금으로 동시 답변 방지 (SELECT ... FOR UPDATE)
        Inquiry inquiry = inquiryRepository.findByIdForUpdate(command.inquiryId())
                .orElseThrow(() -> new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND));

        if (inquiry.getStatus() == InquiryStatus.ANSWERED) {
            throw new InquiryException(InquiryErrorCode.ALREADY_ANSWERED);
        }

        inquiry.answer(command.reply());

        // FCM은 트랜잭션 커밋 후 발송 — DB 커넥션 점유 방지
        Long inquiryId = inquiry.getId();
        Long userId = inquiry.getUserId();
        String adminReply = inquiry.getAdminReply();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendNotification(inquiryId, userId, adminReply);
            }
        });
    }

    private void sendNotification(Long inquiryId, Long userId, String adminReply) {
        try {
            List<String> tokens = userFcmTokenRepository
                    .findByUser_IdIn(List.of(userId))
                    .stream()
                    .map(UserFcmToken::getTokenValue)
                    .toList();

            if (tokens.isEmpty()) {
                log.info("FCM 토큰 없음, 알림 미발송 — inquiryId={}, userId={}",
                        inquiryId, userId);
                return;
            }

            String title = "문의 답변이 도착했어요";
            String body = truncate(adminReply, 100);

            FcmMessageSender.SendResult result = fcmMessageSender.sendToTokens(
                    tokens, title, body,
                    Map.of("type", "INQUIRY_REPLY", "inquiryId", String.valueOf(inquiryId))
            );

            log.info("문의 답변 FCM 발송 — inquiryId={}, success={}, failure={}",
                    inquiryId, result.successCount(), result.failureCount());
        } catch (Exception e) {
            // FCM 실패해도 답변 저장은 이미 커밋됨
            log.error("FCM 발송 실패 — inquiryId={}, userId={}", inquiryId, userId, e);
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "…";
    }
}
