package com.example.freshkitchen.application.auth.usecase;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;

public interface RefreshTokenUseCase {

    AuthTokenResult refresh(Command command);

    record Command(String refreshToken) {
    }
}
