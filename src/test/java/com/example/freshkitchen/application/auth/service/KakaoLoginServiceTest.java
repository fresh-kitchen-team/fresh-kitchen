package com.example.freshkitchen.application.auth.service;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.KakaoLoginUseCase;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.exception.OAuthErrorCode;
import com.example.freshkitchen.global.security.exception.OAuthException;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.infrastructure.oauth.KakaoTokenVerifier;
import com.example.freshkitchen.infrastructure.oauth.KakaoTokenVerifier.KakaoUserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class KakaoLoginServiceTest {

    private final KakaoTokenVerifier kakaoTokenVerifier = mock(KakaoTokenVerifier.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);

    private final KakaoLoginService service = new KakaoLoginService(
            kakaoTokenVerifier, userRepository, jwtTokenProvider
    );

    @Test
    void login_returnsTokens_whenExistingUser() {
        KakaoUserInfo userInfo = new KakaoUserInfo("kakao-sub-123", "user@kakao.com");
        given(kakaoTokenVerifier.verify("valid-id-token")).willReturn(userInfo);

        User existingUser = User.create(new User.CreateCommand("kakao-sub-123", Provider.KAKAO));
        ReflectionTestUtils.setField(existingUser, "id", 1L);
        given(userRepository.findByProviderAndProviderUserId(Provider.KAKAO, "kakao-sub-123"))
                .willReturn(Optional.of(existingUser));
        given(jwtTokenProvider.generateAccessToken(existingUser.getId(), Role.USER))
                .willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(existingUser.getId()))
                .willReturn("refresh-token");

        AuthTokenResult result = service.login(new KakaoLoginUseCase.Command("valid-id-token"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.newUser()).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_createsUserAndReturnsTokens_whenNewUser() {
        KakaoUserInfo userInfo = new KakaoUserInfo("new-kakao-sub", "new@kakao.com");
        given(kakaoTokenVerifier.verify("new-user-token")).willReturn(userInfo);
        given(userRepository.findByProviderAndProviderUserId(Provider.KAKAO, "new-kakao-sub"))
                .willReturn(Optional.empty());

        User savedUser = User.create(new User.CreateCommand("new-kakao-sub", Provider.KAKAO));
        ReflectionTestUtils.setField(savedUser, "id", 2L);
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtTokenProvider.generateAccessToken(savedUser.getId(), Role.USER))
                .willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(savedUser.getId()))
                .willReturn("new-refresh-token");

        AuthTokenResult result = service.login(new KakaoLoginUseCase.Command("new-user-token"));

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.newUser()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_returnsExistingUser_whenRaceConditionOnSave() {
        KakaoUserInfo userInfo = new KakaoUserInfo("race-sub", "race@kakao.com");
        given(kakaoTokenVerifier.verify("race-token")).willReturn(userInfo);
        given(userRepository.findByProviderAndProviderUserId(Provider.KAKAO, "race-sub"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        User existingUser = User.create(new User.CreateCommand("race-sub", Provider.KAKAO));
        ReflectionTestUtils.setField(existingUser, "id", 3L);

        given(userRepository.findByProviderAndProviderUserId(Provider.KAKAO, "race-sub"))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(existingUser));
        given(jwtTokenProvider.generateAccessToken(3L, Role.USER)).willReturn("race-access");
        given(jwtTokenProvider.generateRefreshToken(3L)).willReturn("race-refresh");

        AuthTokenResult result = service.login(new KakaoLoginUseCase.Command("race-token"));

        assertThat(result.accessToken()).isEqualTo("race-access");
        assertThat(result.refreshToken()).isEqualTo("race-refresh");
        assertThat(result.newUser()).isFalse();
    }

    @Test
    void login_throwsOAuthException_whenIdTokenIsInvalid() {
        given(kakaoTokenVerifier.verify("invalid-token"))
                .willThrow(new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN));

        assertThatThrownBy(() -> service.login(new KakaoLoginUseCase.Command("invalid-token")))
                .isInstanceOf(OAuthException.class)
                .satisfies(ex -> assertThat(((OAuthException) ex).getErrorCode())
                        .isEqualTo(OAuthErrorCode.INVALID_ID_TOKEN));
    }
}
