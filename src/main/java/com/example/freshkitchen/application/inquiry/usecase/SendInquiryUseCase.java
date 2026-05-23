package com.example.freshkitchen.application.inquiry.usecase;

import org.springframework.web.multipart.MultipartFile;

public interface SendInquiryUseCase {

    Long send(Command command);

    record Command(
            Long userId,
            InquiryType type,
            InquiryCategory category,
            String content,
            MultipartFile image
    ) {
    }
}
