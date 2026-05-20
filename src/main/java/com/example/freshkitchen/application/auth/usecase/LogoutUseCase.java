package com.example.freshkitchen.application.auth.usecase;

public interface LogoutUseCase {

    void logout(Command command);

    record Command(Long userId, String accessToken) {
    }
}
