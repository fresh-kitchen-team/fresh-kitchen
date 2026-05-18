package com.example.freshkitchen.application.legal.usecase;

public interface AgreeTermsUseCase {

    AgreementResult agree(Long userId);

    record AgreementResult(
            String termsAgreedAt,
            String privacyAgreedAt
    ) {
    }
}
