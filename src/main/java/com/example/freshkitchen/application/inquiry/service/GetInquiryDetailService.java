package com.example.freshkitchen.application.inquiry.service;

import com.example.freshkitchen.application.inquiry.exception.InquiryErrorCode;
import com.example.freshkitchen.application.inquiry.exception.InquiryException;
import com.example.freshkitchen.application.inquiry.usecase.GetInquiryDetailUseCase;
import com.example.freshkitchen.domain.inquiry.entity.Inquiry;
import com.example.freshkitchen.domain.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetInquiryDetailService implements GetInquiryDetailUseCase {

    private final InquiryRepository inquiryRepository;

    @Override
    public Inquiry getDetail(Command command) {
        Inquiry inquiry = inquiryRepository.findById(command.inquiryId())
                .orElseThrow(() -> new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND));

        if (!inquiry.getUserId().equals(command.userId())) {
            throw new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND);
        }

        return inquiry;
    }
}
