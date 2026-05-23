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

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReplyInquiryService implements ReplyInquiryUseCase {

    private final InquiryRepository inquiryRepository;
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final FcmMessageSender fcmMessageSender;

    @Override
    public void reply(Command command) {
        Inquiry inquiry = inquiryRepository.findById(command.inquiryId())
                .orElseThrow(() -> new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND));

        if (inquiry.getStatus() == InquiryStatus.ANSWERED) {
            throw new InquiryException(InquiryErrorCode.ALREADY_ANSWERED);
        }

        inquiry.answer(command.reply());

        sendNotification(inquiry);
    }

    private void sendNotification(Inquiry inquiry) {
        List<String> tokens = userFcmTokenRepository
                .findByUser_IdIn(List.of(inquiry.getUserId()))
                .stream()
                .map(UserFcmToken::getTokenValue)
                .toList();

        if (tokens.isEmpty()) {
            log.info("FCM 토큰 없음, 알림 미발송 — inquiryId={}, userId={}",
                    inquiry.getId(), inquiry.getUserId());
            return;
        }

        String title = "문의 답변이 도착했어요";
        String body = truncate(inquiry.getAdminReply(), 100);

        FcmMessageSender.SendResult result = fcmMessageSender.sendToTokens(
                tokens, title, body,
                Map.of("type", "INQUIRY_REPLY", "inquiryId", String.valueOf(inquiry.getId()))
        );

        log.info("문의 답변 FCM 발송 — inquiryId={}, success={}, failure={}",
                inquiry.getId(), result.successCount(), result.failureCount());
    }

    private static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "…";
    }
}
