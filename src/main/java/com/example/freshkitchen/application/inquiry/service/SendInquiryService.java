package com.example.freshkitchen.application.inquiry.service;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.inquiry.enums.InquiryType;
import com.example.freshkitchen.application.inquiry.usecase.SendInquiryUseCase;
import com.example.freshkitchen.domain.inquiry.entity.Inquiry;
import com.example.freshkitchen.domain.inquiry.repository.InquiryRepository;
import com.example.freshkitchen.infrastructure.inquiry.InquiryProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import com.example.freshkitchen.global.exception.BusinessValidationException;
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
    private final MultipartImageStoragePort imageStorage;

    @Override
    @Transactional
    public Long send(Command command) {
        if (command == null) throw new BusinessValidationException("command must not be null");
        if (command.userId() == null) throw new BusinessValidationException("userId must not be null");
        if (command.type() == null) throw new BusinessValidationException("type must not be null");
        if (command.category() == null) throw new BusinessValidationException("category must not be null");
        if (command.content() == null || command.content().isBlank()) throw new BusinessValidationException("content must not be blank");

        // 1. 이미지 업로드 (있는 경우)
        String imageUrl = null;
        String objectKey = null;
        if (command.image() != null && !command.image().isEmpty()) {
            try {
                MultipartImageStoragePort.StoredImage stored = imageStorage.store(
                        new MultipartImageStoragePort.Command(
                                command.userId(),
                                ImageKind.INQUIRY,
                                command.image().getOriginalFilename(),
                                command.image().getContentType(),
                                command.image().getBytes()
                        )
                );
                imageUrl = stored.imageUrl();
                objectKey = stored.objectKey();
            } catch (Exception e) {
                log.error("문의 이미지 업로드 실패 — userId={}", command.userId(), e);
                // 이미지 업로드 실패해도 문의 자체는 저장
            }
        }

        // 2. DB 저장 — 실패 시 업로드된 이미지 보상 삭제
        Inquiry inquiry;
        try {
            inquiry = inquiryRepository.save(Inquiry.create(new Inquiry.CreateCommand(
                    command.userId(),
                    command.type(),
                    command.category(),
                    command.content(),
                    imageUrl
            )));
        } catch (Exception e) {
            if (objectKey != null) {
                try {
                    imageStorage.delete(new MultipartImageStoragePort.DeleteCommand(objectKey));
                    log.info("DB 저장 실패로 업로드 이미지 보상 삭제 — objectKey={}", objectKey);
                } catch (Exception deleteEx) {
                    log.error("업로드 이미지 보상 삭제 실패 — objectKey={}", objectKey, deleteEx);
                }
            }
            throw e;
        }

        // 3. 메일 발송은 트랜잭션 커밋 후 — DB 커넥션 점유 방지
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
