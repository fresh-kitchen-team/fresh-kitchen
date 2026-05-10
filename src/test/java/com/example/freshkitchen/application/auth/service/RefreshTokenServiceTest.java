package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.RefreshTokenUseCase;
import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import com.example.freshkitchen.global.security.exception.JwtTokenException;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RefreshTokenServiceTest {

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);

    private final RefreshTokenService service = createService();

    private RefreshTokenService createService() {
        RefreshTokenService svc = new RefreshTokenService(jwtTokenProvider, refreshTokenRepository);
        ReflectionTestUtils.setField(svc, "refreshExpirationDays", 14L);
        return svc;
    }

    @Test
    void refresh_returnsNewTokens_whenCasSucceeds() {
        String oldRefreshToken = "old-refresh-token";
        given(jwtTokenProvider.validateRefreshToken(oldRefreshToken)).willReturn(1L);
        given(jwtTokenProvider.generateAccessToken(eq(1L), any())).willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("new-refresh-token");
        given(refreshTokenRepository.compareAndSwap(eq(1L), eq(oldRefreshToken), eq("new-refresh-token"), any()))
                .willReturn(1L);

        AuthTokenResult result = service.refresh(new RefreshTokenUseCase.Command(oldRefreshToken));

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.newUser()).isFalse();
    }

    @Test
    void refresh_deletesAndThrows_whenTokenMismatch() {
        String oldRefreshToken = "stolen-token";
        given(jwtTokenProvider.validateRefreshToken(oldRefreshToken)).willReturn(1L);
        given(jwtTokenProvider.generateAccessToken(eq(1L), any())).willReturn("new-access");
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("new-refresh");
        given(refreshTokenRepository.compareAndSwap(eq(1L), eq(oldRefreshToken), eq("new-refresh"), any()))
                .willReturn(-1L);

        assertThatThrownBy(() -> service.refresh(new RefreshTokenUseCase.Command(oldRefreshToken)))
                .isInstanceOf(JwtTokenException.class)
                .satisfies(ex -> assertThat(((JwtTokenException) ex).getErrorCode())
                        .isEqualTo(JwtErrorCode.INVALID_REFRESH_TOKEN));

        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    @Test
    void refresh_throwsWithoutDelete_whenKeyMissing() {
        String oldRefreshToken = "expired-token";
        given(jwtTokenProvider.validateRefreshToken(oldRefreshToken)).willReturn(1L);
        given(jwtTokenProvider.generateAccessToken(eq(1L), any())).willReturn("new-access");
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("new-refresh");
        given(refreshTokenRepository.compareAndSwap(eq(1L), eq(oldRefreshToken), eq("new-refresh"), any()))
                .willReturn(0L);

        assertThatThrownBy(() -> service.refresh(new RefreshTokenUseCase.Command(oldRefreshToken)))
                .isInstanceOf(JwtTokenException.class)
                .satisfies(ex -> assertThat(((JwtTokenException) ex).getErrorCode())
                        .isEqualTo(JwtErrorCode.INVALID_REFRESH_TOKEN));

        verify(refreshTokenRepository, never()).deleteByUserId(1L);
    }

    @Test
    void refresh_throws_whenRefreshTokenSignatureInvalid() {
        given(jwtTokenProvider.validateRefreshToken("invalid-token"))
                .willThrow(new JwtTokenException(JwtErrorCode.INVALID_SIGNATURE));

        assertThatThrownBy(() -> service.refresh(new RefreshTokenUseCase.Command("invalid-token")))
                .isInstanceOf(JwtTokenException.class)
                .satisfies(ex -> assertThat(((JwtTokenException) ex).getErrorCode())
                        .isEqualTo(JwtErrorCode.INVALID_SIGNATURE));
    }

    @Test
    void refresh_throws_whenRefreshTokenExpired() {
        given(jwtTokenProvider.validateRefreshToken("expired-token"))
                .willThrow(new JwtTokenException(JwtErrorCode.EXPIRED_TOKEN));

        assertThatThrownBy(() -> service.refresh(new RefreshTokenUseCase.Command("expired-token")))
                .isInstanceOf(JwtTokenException.class)
                .satisfies(ex -> assertThat(((JwtTokenException) ex).getErrorCode())
                        .isEqualTo(JwtErrorCode.EXPIRED_TOKEN));
    }

    @Test
    void refresh_throws_whenRefreshTokenMalformed() {
        given(jwtTokenProvider.validateRefreshToken("malformed-token"))
                .willThrow(new JwtTokenException(JwtErrorCode.MALFORMED_TOKEN));

        assertThatThrownBy(() -> service.refresh(new RefreshTokenUseCase.Command("malformed-token")))
                .isInstanceOf(JwtTokenException.class)
                .satisfies(ex -> assertThat(((JwtTokenException) ex).getErrorCode())
                        .isEqualTo(JwtErrorCode.MALFORMED_TOKEN));
    }

    @Test
    void refresh_passesCorrectDurationToCompareAndSwap() {
        String oldToken = "old-token-ttl";
        given(jwtTokenProvider.validateRefreshToken(oldToken)).willReturn(7L);
        given(jwtTokenProvider.generateAccessToken(eq(7L), any())).willReturn("access");
        given(jwtTokenProvider.generateRefreshToken(7L)).willReturn("new-refresh");
        given(refreshTokenRepository.compareAndSwap(eq(7L), eq(oldToken), eq("new-refresh"), eq(java.time.Duration.ofDays(14L))))
                .willReturn(1L);

        AuthTokenResult result = service.refresh(new RefreshTokenUseCase.Command(oldToken));

        assertThat(result.newUser()).isFalse();
        verify(refreshTokenRepository).compareAndSwap(
                eq(7L), eq(oldToken), eq("new-refresh"), eq(java.time.Duration.ofDays(14L))
        );
    }

    @Test
    void refresh_generatesAccessTokenWithUserRole() {
        String oldToken = "role-check-token";
        given(jwtTokenProvider.validateRefreshToken(oldToken)).willReturn(9L);
        given(jwtTokenProvider.generateAccessToken(eq(9L), any())).willReturn("access");
        given(jwtTokenProvider.generateRefreshToken(9L)).willReturn("new-refresh");
        given(refreshTokenRepository.compareAndSwap(any(), any(), any(), any())).willReturn(1L);

        service.refresh(new RefreshTokenUseCase.Command(oldToken));

        verify(jwtTokenProvider).generateAccessToken(9L, com.example.freshkitchen.global.security.Role.USER);
    }

    @Test
    void refresh_doesNotDeleteSession_whenKeyMissing() {
        String oldToken = "missing-key-token";
        given(jwtTokenProvider.validateRefreshToken(oldToken)).willReturn(2L);
        given(jwtTokenProvider.generateAccessToken(any(), any())).willReturn("access");
        given(jwtTokenProvider.generateRefreshToken(any())).willReturn("new-refresh");
        given(refreshTokenRepository.compareAndSwap(eq(2L), eq(oldToken), eq("new-refresh"), any()))
                .willReturn(0L);

        assertThatThrownBy(() -> service.refresh(new RefreshTokenUseCase.Command(oldToken)))
                .isInstanceOf(JwtTokenException.class);

        verify(refreshTokenRepository, never()).deleteByUserId(2L);
    }
}
