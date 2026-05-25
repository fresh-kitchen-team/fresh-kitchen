package com.example.freshkitchen.application.inquiry.usecase;

import com.example.freshkitchen.domain.inquiry.enums.InquiryCategory;
import com.example.freshkitchen.domain.inquiry.enums.InquiryType;
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
