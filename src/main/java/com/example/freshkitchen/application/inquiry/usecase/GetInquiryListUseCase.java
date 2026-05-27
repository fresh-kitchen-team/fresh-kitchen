package com.example.freshkitchen.application.inquiry.usecase;

import com.example.freshkitchen.application.inquiry.dto.InquiryResult;

import java.util.List;

public interface GetInquiryListUseCase {

    List<InquiryResult> getList(Command command);

    record Command(Long userId) {
    }
}
