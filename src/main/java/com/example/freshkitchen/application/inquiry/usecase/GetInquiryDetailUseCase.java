package com.example.freshkitchen.application.inquiry.usecase;

import com.example.freshkitchen.application.inquiry.dto.InquiryResult;

public interface GetInquiryDetailUseCase {

    InquiryResult getDetail(Command command);

    record Command(Long userId, Long inquiryId) {
    }
}
