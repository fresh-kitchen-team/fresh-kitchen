package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.GoogleLoginUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.exception.OAuthErrorCode;
import com.example.freshkitchen.global.security.exception.OAuthException;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import com.example.freshkitchen.infrastructure.oauth.GoogleTokenVerifier;
import com.example.freshkitchen.infrastructure.oauth.GoogleTokenVerifier.GoogleUserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GoogleLoginServiceTest {

    private final GoogleTokenVerifier googleTokenVerifier = mock(GoogleTokenVerifier.class);
    private final OAuthUserResolver oAuthUserResolver = mock(OAuthUserResolver.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);

    private final GoogleLoginService service = createService();

    private GoogleLoginService createService() {
        GoogleLoginService svc = new GoogleLoginService(
                googleTokenVerifier, oAuthUserResolver, jwtTokenProvider, refreshTokenRepository
        );
        ReflectionTestUtils.setField(svc, "refreshExpirationDays", 14L);
        return svc;
    }

    @Test
    void login_returnsTokens_whenExistingUser() {
        GoogleUserInfo userInfo = new GoogleUserInfo("google-sub-123", "user@gmail.com");
        given(googleTokenVerifier.verify("valid-id-token")).willReturn(userInfo);

        User existingUser = User.create(new User.CreateCommand("google-sub-123", Provider.GOOGLE));
        ReflectionTestUtils.setField(existingUser, "id", 1L);
        given(oAuthUserResolver.resolve("google-sub-123", Provider.GOOGLE))
                .willReturn(new OAuthUserResolver.Result(existingUser, false));
        given(jwtTokenProvider.generateAccessToken(1L, Role.USER)).willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("refresh-token");

        AuthTokenResult result = service.login(new GoogleLoginUseCase.Command("valid-id-token"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.newUser()).isFalse();
        verify(refreshTokenRepository).save(1L, "refresh-token", java.time.Duration.ofDays(14L));
    }

    @Test
    void login_createsUserAndReturnsTokens_whenNewUser() {
        GoogleUserInfo userInfo = new GoogleUserInfo("new-google-sub", "new@gmail.com");
        given(googleTokenVerifier.verify("new-user-token")).willReturn(userInfo);

        User savedUser = User.create(new User.CreateCommand("new-google-sub", Provider.GOOGLE));
        ReflectionTestUtils.setField(savedUser, "id", 2L);
        given(oAuthUserResolver.resolve("new-google-sub", Provider.GOOGLE))
                .willReturn(new OAuthUserResolver.Result(savedUser, true));
        given(jwtTokenProvider.generateAccessToken(2L, Role.USER)).willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(2L)).willReturn("new-refresh-token");

        AuthTokenResult result = service.login(new GoogleLoginUseCase.Command("new-user-token"));

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.newUser()).isTrue();
        verify(refreshTokenRepository).save(2L, "new-refresh-token", java.time.Duration.ofDays(14L));
    }

    @Test
    void login_throwsOAuthException_whenIdTokenIsInvalid() {
        given(googleTokenVerifier.verify("invalid-token"))
                .willThrow(new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN));

        assertThatThrownBy(() -> service.login(new GoogleLoginUseCase.Command("invalid-token")))
                .isInstanceOf(OAuthException.class)
                .satisfies(ex -> assertThat(((OAuthException) ex).getErrorCode())
                        .isEqualTo(OAuthErrorCode.INVALID_ID_TOKEN));
    }
}
