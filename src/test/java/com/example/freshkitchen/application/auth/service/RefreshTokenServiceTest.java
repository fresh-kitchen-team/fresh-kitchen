package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.RefreshTokenUseCase;
import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import com.example.freshkitchen.global.security.exception.JwtTokenException;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
    void refresh_returnsNewTokens_whenRefreshTokenIsValid() {
        String oldRefreshToken = "old-refresh-token";
        given(jwtTokenProvider.validateRefreshToken(oldRefreshToken)).willReturn(1L);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of(oldRefreshToken));
        given(jwtTokenProvider.generateAccessToken(eq(1L), any())).willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("new-refresh-token");

        AuthTokenResult result = service.refresh(new RefreshTokenUseCase.Command(oldRefreshToken));

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.newUser()).isFalse();
        verify(refreshTokenRepository).save(eq(1L), eq("new-refresh-token"), any());
    }

    @Test
    void refresh_throws_whenRefreshTokenNotInRedis() {
        given(jwtTokenProvider.validateRefreshToken("unknown-token")).willReturn(1L);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(new RefreshTokenUseCase.Command("unknown-token")))
                .isInstanceOf(JwtTokenException.class)
                .satisfies(ex -> assertThat(((JwtTokenException) ex).getErrorCode())
                        .isEqualTo(JwtErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void refresh_deletesAndThrows_whenRefreshTokenMismatch() {
        given(jwtTokenProvider.validateRefreshToken("stolen-token")).willReturn(1L);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of("real-token"));

        assertThatThrownBy(() -> service.refresh(new RefreshTokenUseCase.Command("stolen-token")))
                .isInstanceOf(JwtTokenException.class)
                .satisfies(ex -> assertThat(((JwtTokenException) ex).getErrorCode())
                        .isEqualTo(JwtErrorCode.INVALID_REFRESH_TOKEN));

        verify(refreshTokenRepository).deleteByUserId(1L);
    }
}
