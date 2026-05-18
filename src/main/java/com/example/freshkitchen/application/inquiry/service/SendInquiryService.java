package com.example.freshkitchen.application.inquiry.service;

import com.example.freshkitchen.application.inquiry.exception.InquiryErrorCode;
import com.example.freshkitchen.application.inquiry.exception.InquiryException;
import com.example.freshkitchen.application.inquiry.usecase.SendInquiryUseCase;
import com.example.freshkitchen.infrastructure.inquiry.InquiryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendInquiryService implements SendInquiryUseCase {

    private final JavaMailSender mailSender;
    private final InquiryProperties inquiryProperties;

    @Override
    public void send(Command command) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(inquiryProperties.getAdminEmail());
        message.setSubject("[FreshKitchen 문의] " + sanitizeHeader(command.title()));
        message.setText(buildBody(command));

        if (command.contactEmail() != null && !command.contactEmail().isBlank()) {
            message.setReplyTo(sanitizeHeader(command.contactEmail()));
        }

        try {
            mailSender.send(message);
            log.info("문의 이메일 발송 완료 — userId={}, title={}", command.userId(), sanitizeHeader(command.title()));
        } catch (MailException e) {
            log.error("문의 이메일 발송 실패 — userId={}, title={}", command.userId(), sanitizeHeader(command.title()), e);
            throw new InquiryException(InquiryErrorCode.MAIL_SEND_FAILED, e);
        }
    }

    private static String sanitizeHeader(String value) {
        return value.replaceAll("[\\r\\n]", " ").trim();
    }

    private static String buildBody(Command command) {
        StringBuilder sb = new StringBuilder();
        sb.append("사용자 ID: ").append(command.userId()).append("\n");
        if (command.contactEmail() != null && !command.contactEmail().isBlank()) {
            sb.append("연락처: ").append(command.contactEmail()).append("\n");
        }
        sb.append("\n---\n\n");
        sb.append(command.content());
        return sb.toString();
    }
}
