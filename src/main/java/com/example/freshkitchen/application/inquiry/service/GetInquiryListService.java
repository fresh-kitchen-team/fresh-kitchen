package com.example.freshkitchen.application.inquiry.service;

import com.example.freshkitchen.application.inquiry.dto.InquiryResult;
import com.example.freshkitchen.application.inquiry.usecase.GetInquiryListUseCase;
import com.example.freshkitchen.domain.inquiry.repository.InquiryRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetInquiryListService implements GetInquiryListUseCase {

    private final InquiryRepository inquiryRepository;

    @Override
    public List<InquiryResult> getList(Command command) {
        if (command == null) throw new BusinessValidationException("command must not be null");
        if (command.userId() == null) throw new BusinessValidationException("userId must not be null");

        return InquiryResult.from(
                inquiryRepository.findByUserIdOrderByCreatedAtDesc(command.userId())
        );
    }
}
