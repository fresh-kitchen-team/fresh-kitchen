package com.example.freshkitchen.application.auth.usecase;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;

public interface GoogleLoginUseCase {

    AuthTokenResult login(Command command);

    record Command(String idToken) {
    }
}
