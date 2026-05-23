package com.example.freshkitchen.application.inquiry.service;

import com.example.freshkitchen.application.inquiry.exception.InquiryErrorCode;
import com.example.freshkitchen.application.inquiry.exception.InquiryException;
import com.example.freshkitchen.domain.inquiry.enums.InquiryType;
import com.example.freshkitchen.application.inquiry.usecase.SendInquiryUseCase;
import com.example.freshkitchen.domain.inquiry.entity.Inquiry;
import com.example.freshkitchen.domain.inquiry.repository.InquiryRepository;
import com.example.freshkitchen.infrastructure.inquiry.InquiryProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SendInquiryService implements SendInquiryUseCase {

    private final JavaMailSender mailSender;
    private final InquiryProperties inquiryProperties;
    private final InquiryRepository inquiryRepository;

    @Override
    public Long send(Command command) {
        // 1. DB 저장
        Inquiry inquiry = inquiryRepository.save(Inquiry.create(new Inquiry.CreateCommand(
                command.userId(),
                command.type(),
                command.category(),
                command.content(),
                null // image URL은 현재 S3 업로드 미사용, 메일 첨부만
        )));

        // 2. 메일 발송
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, command.image() != null);

            String prefix = command.type() == InquiryType.REPORT
                    ? "[FreshKitchen 신고]"
                    : "[FreshKitchen 문의]";

            helper.setTo(inquiryProperties.getAdminEmail());
            helper.setSubject(prefix + " " + sanitize(command.category().name()));
            helper.setText(buildBody(command, inquiry.getId()));

            if (command.image() != null && !command.image().isEmpty()) {
                String originalFilename = command.image().getOriginalFilename();
                String safeFilename = (originalFilename != null)
                        ? org.springframework.util.StringUtils.getFilename(originalFilename)
                        : "attachment";
                if (safeFilename == null || safeFilename.isBlank()) {
                    safeFilename = "attachment";
                }
                helper.addAttachment(safeFilename, command.image());
            }

            mailSender.send(mimeMessage);
            log.info("문의 이메일 발송 완료 — inquiryId={}, userId={}, type={}, category={}",
                    inquiry.getId(), command.userId(), command.type(), command.category());
        } catch (MailException | MessagingException e) {
            // 메일 실패해도 DB 저장은 유지 — 문의 내역은 보존
            log.error("문의 이메일 발송 실패 — inquiryId={}, userId={}, type={}, category={}",
                    inquiry.getId(), command.userId(), command.type(), command.category(), e);
        }

        return inquiry.getId();
    }

    private static String sanitize(String value) {
        return value.replaceAll("[\\r\\n]", " ").trim();
    }

    private static String buildBody(Command command, Long inquiryId) {
        StringBuilder sb = new StringBuilder();
        sb.append("문의 ID: ").append(inquiryId).append("\n");
        sb.append("유형: ").append(command.type() == InquiryType.REPORT ? "신고" : "문의").append("\n");
        sb.append("카테고리: ").append(command.category().name()).append("\n");
        sb.append("사용자 ID: ").append(command.userId()).append("\n");
        sb.append("\n---\n\n");
        sb.append(command.content());
        return sb.toString();
    }
}
