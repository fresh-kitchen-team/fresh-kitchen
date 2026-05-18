package com.example.freshkitchen.application.inquiry.service;

import com.example.freshkitchen.application.inquiry.exception.InquiryErrorCode;
import com.example.freshkitchen.application.inquiry.exception.InquiryException;
import com.example.freshkitchen.application.inquiry.usecase.InquiryType;
import com.example.freshkitchen.application.inquiry.usecase.SendInquiryUseCase;
import com.example.freshkitchen.infrastructure.inquiry.InquiryProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendInquiryService implements SendInquiryUseCase {

    private final JavaMailSender mailSender;
    private final InquiryProperties inquiryProperties;

    @Override
    public void send(Command command) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, command.image() != null);

            String prefix = command.type() == InquiryType.REPORT
                    ? "[FreshKitchen 신고]"
                    : "[FreshKitchen 문의]";

            helper.setTo(inquiryProperties.getAdminEmail());
            helper.setSubject(prefix + " " + sanitize(command.category().name()));
            helper.setText(buildBody(command));

            if (command.image() != null && !command.image().isEmpty()) {
                helper.addAttachment(
                        command.image().getOriginalFilename(),
                        command.image()
                );
            }

            mailSender.send(mimeMessage);
            log.info("문의 이메일 발송 완료 — userId={}, type={}, category={}",
                    command.userId(), command.type(), command.category());
        } catch (MailException | MessagingException e) {
            log.error("문의 이메일 발송 실패 — userId={}, type={}, category={}",
                    command.userId(), command.type(), command.category(), e);
            throw new InquiryException(InquiryErrorCode.MAIL_SEND_FAILED, e);
        }
    }

    private static String sanitize(String value) {
        return value.replaceAll("[\\r\\n]", " ").trim();
    }

    private static String buildBody(Command command) {
        StringBuilder sb = new StringBuilder();
        sb.append("유형: ").append(command.type() == InquiryType.REPORT ? "신고" : "문의").append("\n");
        sb.append("카테고리: ").append(command.category().name()).append("\n");
        sb.append("사용자 ID: ").append(command.userId()).append("\n");
        sb.append("\n---\n\n");
        sb.append(command.content());
        return sb.toString();
    }
}
