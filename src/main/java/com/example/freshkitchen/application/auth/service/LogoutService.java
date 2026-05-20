package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.usecase.LogoutUseCase;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.auth.AccessTokenBlacklistRepository;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 로그아웃 처리. 멱등 — 이미 로그아웃된 유저에 대해 재호출해도 안전합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void logout(Command command) {
        if (command == null || command.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        try {
            refreshTokenRepository.deleteByUserId(command.userId());

            if (command.accessToken() != null) {
                Duration remaining = jwtTokenProvider.getRemainingTtl(command.accessToken());
                accessTokenBlacklistRepository.add(command.accessToken(), remaining);
            }

            log.info("로그아웃 완료. userId={}", command.userId());
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            log.error("Redis 연결 실패로 로그아웃 토큰 무효화 불완전. userId={}", command.userId(), e);
            // 로그아웃은 best-effort — 클라이언트가 토큰을 삭제하면 UX적으로 로그아웃 완료
            // Refresh Token은 Redis TTL(14일)로 자연 만료됨
        }
    }
}
