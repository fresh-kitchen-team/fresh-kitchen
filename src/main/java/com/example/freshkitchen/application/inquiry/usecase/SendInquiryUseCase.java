package com.example.freshkitchen.application.inquiry.usecase;

import org.springframework.web.multipart.MultipartFile;

public interface SendInquiryUseCase {

    void send(Command command);

    record Command(
            Long userId,
            InquiryType type,
            InquiryCategory category,
            String content,
            MultipartFile image
    ) {
    }
}
