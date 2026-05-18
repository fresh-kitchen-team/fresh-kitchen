package com.example.freshkitchen.application.legal.usecase;

import java.time.OffsetDateTime;

public interface GetTermsAgreementUseCase {

    AgreementStatus getStatus(Long userId);

    record AgreementStatus(
            boolean termsAgreed,
            boolean privacyAgreed,
            OffsetDateTime termsAgreedAt,
            OffsetDateTime privacyAgreedAt
    ) {
    }
}
