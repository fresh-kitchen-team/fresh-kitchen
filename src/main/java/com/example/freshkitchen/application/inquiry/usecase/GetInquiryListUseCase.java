package com.example.freshkitchen.application.inquiry.usecase;

import com.example.freshkitchen.domain.inquiry.entity.Inquiry;

import java.util.List;

public interface GetInquiryListUseCase {

    List<Inquiry> getList(Command command);

    record Command(Long userId) {
    }
}
