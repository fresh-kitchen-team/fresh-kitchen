package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.GoogleLoginUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.exception.OAuthErrorCode;
import com.example.freshkitchen.global.security.exception.OAuthException;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.auth.RefreshTokenRepository;
import com.example.freshkitchen.infrastructure.oauth.GoogleTokenVerifier;
import com.example.freshkitchen.infrastructure.oauth.GoogleTokenVerifier.GoogleUserInfo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class GoogleLoginServiceTest {

    private final GoogleTokenVerifier googleTokenVerifier = mock(GoogleTokenVerifier.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);

    private final GoogleLoginService service = createService();

    private GoogleLoginService createService() {
        GoogleLoginService svc = new GoogleLoginService(
                googleTokenVerifier, userRepository, jwtTokenProvider, refreshTokenRepository
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
        given(userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, "google-sub-123"))
                .willReturn(Optional.of(existingUser));
        given(jwtTokenProvider.generateAccessToken(existingUser.getId(), Role.USER))
                .willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(existingUser.getId()))
                .willReturn("refresh-token");

        AuthTokenResult result = service.login(new GoogleLoginUseCase.Command("valid-id-token"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.newUser()).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_createsUserAndReturnsTokens_whenNewUser() {
        GoogleUserInfo userInfo = new GoogleUserInfo("new-google-sub", "new@gmail.com");
        given(googleTokenVerifier.verify("new-user-token")).willReturn(userInfo);
        given(userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, "new-google-sub"))
                .willReturn(Optional.empty());

        User savedUser = User.create(new User.CreateCommand("new-google-sub", Provider.GOOGLE));
        ReflectionTestUtils.setField(savedUser, "id", 2L);
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtTokenProvider.generateAccessToken(savedUser.getId(), Role.USER))
                .willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(savedUser.getId()))
                .willReturn("new-refresh-token");

        AuthTokenResult result = service.login(new GoogleLoginUseCase.Command("new-user-token"));

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.newUser()).isTrue();
        verify(userRepository).save(any(User.class));
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

    @Test
    void login_recoversFromRaceCondition_whenDataIntegrityViolationFollowedBySuccessfulRefetch() {
        GoogleUserInfo userInfo = new GoogleUserInfo("race-sub", "race@gmail.com");
        given(googleTokenVerifier.verify("race-token")).willReturn(userInfo);
        given(userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, "race-sub"))
                .willReturn(Optional.empty());

        User racingUser = User.create(new User.CreateCommand("race-sub", Provider.GOOGLE));
        ReflectionTestUtils.setField(racingUser, "id", 3L);
        given(userRepository.save(any(User.class)))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));
        given(userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, "race-sub"))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(racingUser));
        given(jwtTokenProvider.generateAccessToken(3L, Role.USER)).willReturn("race-access-token");
        given(jwtTokenProvider.generateRefreshToken(3L)).willReturn("race-refresh-token");

        AuthTokenResult result = service.login(new GoogleLoginUseCase.Command("race-token"));

        assertThat(result.accessToken()).isEqualTo("race-access-token");
        assertThat(result.refreshToken()).isEqualTo("race-refresh-token");
        assertThat(result.newUser()).isFalse();
    }

    @Test
    void login_rethrowsDataIntegrityViolation_whenRefetchAlsoFails() {
        GoogleUserInfo userInfo = new GoogleUserInfo("ghost-sub", "ghost@gmail.com");
        given(googleTokenVerifier.verify("ghost-token")).willReturn(userInfo);
        given(userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, "ghost-sub"))
                .willReturn(Optional.empty());

        var duplicateEx = new org.springframework.dao.DataIntegrityViolationException("duplicate key");
        given(userRepository.save(any(User.class))).willThrow(duplicateEx);
        given(userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, "ghost-sub"))
                .willReturn(Optional.empty())
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new GoogleLoginUseCase.Command("ghost-token")))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void login_savesRefreshTokenWithCorrectUserIdAndTtl() {
        GoogleUserInfo userInfo = new GoogleUserInfo("save-sub", "save@gmail.com");
        given(googleTokenVerifier.verify("save-token")).willReturn(userInfo);

        User existingUser = User.create(new User.CreateCommand("save-sub", Provider.GOOGLE));
        ReflectionTestUtils.setField(existingUser, "id", 5L);
        given(userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, "save-sub"))
                .willReturn(Optional.of(existingUser));
        given(jwtTokenProvider.generateAccessToken(5L, Role.USER)).willReturn("access");
        given(jwtTokenProvider.generateRefreshToken(5L)).willReturn("refresh-to-save");

        service.login(new GoogleLoginUseCase.Command("save-token"));

        verify(refreshTokenRepository).save(
                eq(5L),
                eq("refresh-to-save"),
                eq(Duration.ofDays(14L))
        );
    }

    @Test
    void login_generatesAccessTokenWithUserRole() {
        GoogleUserInfo userInfo = new GoogleUserInfo("role-sub", "role@gmail.com");
        given(googleTokenVerifier.verify("role-token")).willReturn(userInfo);

        User existingUser = User.create(new User.CreateCommand("role-sub", Provider.GOOGLE));
        ReflectionTestUtils.setField(existingUser, "id", 6L);
        given(userRepository.findByProviderAndProviderUserId(Provider.GOOGLE, "role-sub"))
                .willReturn(Optional.of(existingUser));
        given(jwtTokenProvider.generateAccessToken(6L, Role.USER)).willReturn("access");
        given(jwtTokenProvider.generateRefreshToken(6L)).willReturn("refresh");

        service.login(new GoogleLoginUseCase.Command("role-token"));

        verify(jwtTokenProvider).generateAccessToken(6L, Role.USER);
    }
}
