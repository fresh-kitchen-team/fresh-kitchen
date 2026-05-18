package com.example.freshkitchen.application.legal.service;

import com.example.freshkitchen.application.legal.usecase.AgreeTermsUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgreeTermsService implements AgreeTermsUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public AgreementResult agree(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        user.agreeTerms();

        return new AgreementResult(
                user.getTermsAgreedAt().toString(),
                user.getPrivacyAgreedAt().toString()
        );
    }
}
