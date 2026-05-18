package com.example.freshkitchen.application.legal.usecase;

public interface GetTermsAgreementUseCase {

    AgreementStatus getStatus(Long userId);

    record AgreementStatus(
            boolean termsAgreed,
            boolean privacyAgreed,
            String termsAgreedAt,
            String privacyAgreedAt
    ) {
    }
}
