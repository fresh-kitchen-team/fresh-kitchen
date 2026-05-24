package com.example.freshkitchen.application.inquiry.service;

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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendInquiryService implements SendInquiryUseCase {

    private final JavaMailSender mailSender;
    private final InquiryProperties inquiryProperties;
    private final InquiryRepository inquiryRepository;

    @Override
    @Transactional
    public Long send(Command command) {
        // 1. DB 저장
        Inquiry inquiry = inquiryRepository.save(Inquiry.create(new Inquiry.CreateCommand(
                command.userId(),
                command.type(),
                command.category(),
                command.content(),
                null // image URL은 현재 S3 업로드 미사용, 메일 첨부만
        )));

        // 2. 메일 발송은 트랜잭션 커밋 후 — DB 커넥션 점유 방지
        Long inquiryId = inquiry.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendMail(command, inquiryId);
            }
        });

        return inquiryId;
    }

    private void sendMail(Command command, Long inquiryId) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, command.image() != null);

            String prefix = command.type() == InquiryType.REPORT
                    ? "[FreshKitchen 신고]"
                    : "[FreshKitchen 문의]";

            helper.setTo(inquiryProperties.getAdminEmail());
            helper.setSubject(prefix + " " + sanitize(command.category().name()));
            helper.setText(buildBody(command, inquiryId));

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
                    inquiryId, command.userId(), command.type(), command.category());
        } catch (MailException | MessagingException e) {
            // 메일 실패해도 DB 저장은 이미 커밋됨 — 문의 내역은 보존
            log.error("문의 이메일 발송 실패 — inquiryId={}, userId={}, type={}, category={}",
                    inquiryId, command.userId(), command.type(), command.category(), e);
        }
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
