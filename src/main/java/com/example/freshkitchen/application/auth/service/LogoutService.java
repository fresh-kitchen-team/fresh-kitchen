package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.usecase.LogoutUseCase;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void logout(Command command) {
        refreshTokenRepository.deleteByUserId(command.userId());
    }
}
