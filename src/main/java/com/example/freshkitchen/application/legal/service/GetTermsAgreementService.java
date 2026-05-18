package com.example.freshkitchen.application.legal.service;

import com.example.freshkitchen.application.legal.usecase.GetTermsAgreementUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTermsAgreementService implements GetTermsAgreementUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public AgreementStatus getStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        return new AgreementStatus(
                user.getTermsAgreedAt() != null,
                user.getPrivacyAgreedAt() != null,
                user.getTermsAgreedAt() != null ? user.getTermsAgreedAt().toString() : null,
                user.getPrivacyAgreedAt() != null ? user.getPrivacyAgreedAt().toString() : null
        );
    }
}
