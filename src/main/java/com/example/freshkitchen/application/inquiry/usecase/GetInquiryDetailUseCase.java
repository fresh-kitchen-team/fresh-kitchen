package com.example.freshkitchen.application.inquiry.usecase;

import com.example.freshkitchen.domain.inquiry.entity.Inquiry;

public interface GetInquiryDetailUseCase {

    Inquiry getDetail(Command command);

    record Command(Long userId, Long inquiryId) {
    }
}
