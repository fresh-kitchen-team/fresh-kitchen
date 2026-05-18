package com.example.freshkitchen.application.legal.usecase;

import java.time.OffsetDateTime;

public interface AgreeTermsUseCase {

    AgreementResult agree(Long userId);

    record AgreementResult(
            OffsetDateTime termsAgreedAt,
            OffsetDateTime privacyAgreedAt
    ) {
    }
}
