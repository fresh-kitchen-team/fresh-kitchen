package com.example.freshkitchen.application.inquiry.service;

import com.example.freshkitchen.application.inquiry.usecase.SendInquiryUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendInquiryService implements SendInquiryUseCase {

    private final JavaMailSender mailSender;

    @Value("${app.inquiry.admin-email}")
    private String adminEmail;

    @Override
    public void send(Command command) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject("[FreshKitchen 문의] " + command.title());
        message.setText(buildBody(command));

        if (command.contactEmail() != null && !command.contactEmail().isBlank()) {
            message.setReplyTo(command.contactEmail());
        }

        mailSender.send(message);
        log.info("문의 이메일 발송 완료 — userId={}, title={}", command.userId(), command.title());
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
