package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.usecase.LogoutUseCase;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.auth.AccessTokenBlacklistRepository;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class LogoutServiceTest {

    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository = mock(AccessTokenBlacklistRepository.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);

    private final LogoutService service = new LogoutService(
            refreshTokenRepository, accessTokenBlacklistRepository, jwtTokenProvider
    );

    @Test
    void logout_deletesRefreshAndBlacklistsAccess() {
        given(jwtTokenProvider.getRemainingTtl("access-token")).willReturn(Duration.ofMinutes(15));

        service.logout(new LogoutUseCase.Command(1L, "access-token"));

        then(refreshTokenRepository).should().deleteByUserId(1L);
        then(accessTokenBlacklistRepository).should().add("access-token", Duration.ofMinutes(15));
    }

    @Test
    void logout_skipsBlacklist_whenAccessTokenIsNull() {
        service.logout(new LogoutUseCase.Command(1L, null));

        then(refreshTokenRepository).should().deleteByUserId(1L);
        then(accessTokenBlacklistRepository).shouldHaveNoInteractions();
    }

    @Test
    void logout_throwsBusinessValidationException_whenCommandIsNull() {
        assertThatThrownBy(() -> service.logout(null))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void logout_throwsBusinessValidationException_whenUserIdIsNull() {
        assertThatThrownBy(() -> service.logout(new LogoutUseCase.Command(null, "token")))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void logout_succeedsGracefully_whenRedisConnectionFails() {
        willThrow(new RedisConnectionFailureException("connection refused"))
                .given(refreshTokenRepository).deleteByUserId(1L);

        // best-effort: should not throw
        service.logout(new LogoutUseCase.Command(1L, "access-token"));

        then(accessTokenBlacklistRepository).should(never()).add(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        );
    }
}
