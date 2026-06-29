package com.example.freshkitchen.application.auth.usecase;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;

public interface DevLoginUseCase {

    record Command(Long userId) {
    }

    AuthTokenResult login(Command command);
}
