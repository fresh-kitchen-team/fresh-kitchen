package com.example.freshkitchen.global.config;

import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import com.example.freshkitchen.global.security.exception.JwtTokenException;
import com.example.freshkitchen.global.security.exception.SecurityErrorCode;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.global.security.infrastructure.TokenPayload;
import com.example.freshkitchen.application.home.usecase.GetHomeSummaryUseCase;
import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.GetIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ListIngredientsUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ListStoragesUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ResolveIngredientDefaultsUseCase;
import com.example.freshkitchen.application.ingredient.usecase.UpdateIngredientUseCase;
import com.example.freshkitchen.application.user.usecase.DeleteUserProfileUseCase;
import com.example.freshkitchen.application.user.usecase.GetUserProfileUseCase;
import com.example.freshkitchen.application.user.usecase.UpdateUserProfileUseCase;

import com.example.freshkitchen.application.auth.usecase.GoogleLoginUseCase;
import com.example.freshkitchen.application.auth.usecase.KakaoLoginUseCase;
import com.example.freshkitchen.application.auth.usecase.RefreshTokenUseCase;

import com.example.freshkitchen.infrastructure.ai.AiServerClient;
import com.example.freshkitchen.application.user.dto.UserProfileResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, SecurityFilterChainIntegrationTest.TestController.class})
class SecurityFilterChainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private GetUserProfileUseCase getUserProfileUseCase;

    @MockitoBean
    private UpdateUserProfileUseCase updateUserProfileUseCase;

    @MockitoBean
    private DeleteUserProfileUseCase deleteUserProfileUseCase;

    @MockitoBean

    private GetHomeSummaryUseCase getHomeSummaryUseCase;

    @MockitoBean
    private CreateIngredientUseCase createIngredientUseCase;

    @MockitoBean
    private UpdateIngredientUseCase updateIngredientUseCase;

    @MockitoBean
    private GetIngredientUseCase getIngredientUseCase;

    @MockitoBean
    private ListIngredientsUseCase listIngredientsUseCase;

    @MockitoBean
    private ResolveIngredientDefaultsUseCase resolveIngredientDefaultsUseCase;

    @MockitoBean
    private ListStoragesUseCase listStoragesUseCase;

    @MockitoBean
    private GoogleLoginUseCase googleLoginUseCase;

    @MockitoBean
    private KakaoLoginUseCase kakaoLoginUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockitoBean
>>>>>>> dev
    private AiServerClient aiServerClient;

    @Test
    void protectedEndpoint_returns401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.AUTHENTICATION_REQUIRED.code()))
                .andExpect(jsonPath("$.message").value(SecurityErrorCode.AUTHENTICATION_REQUIRED.message()))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/test/protected"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void protectedEndpoint_returns401_whenTokenIsExpired() throws Exception {
        given(jwtTokenProvider.validateAccessToken("expired-token"))
                .willThrow(new JwtTokenException(JwtErrorCode.EXPIRED_TOKEN));

        mockMvc.perform(get("/test/protected")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(JwtErrorCode.EXPIRED_TOKEN.code()))
                .andExpect(jsonPath("$.message").value(JwtErrorCode.EXPIRED_TOKEN.message()));
    }

    @Test
    void protectedEndpoint_returns401_whenSignatureIsInvalid() throws Exception {
        given(jwtTokenProvider.validateAccessToken("tampered-token"))
                .willThrow(new JwtTokenException(JwtErrorCode.INVALID_SIGNATURE));

        mockMvc.perform(get("/test/protected")
                        .header("Authorization", "Bearer tampered-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(JwtErrorCode.INVALID_SIGNATURE.code()));
    }

    @Test
    void protectedEndpoint_allowsAccess_whenValidTokenProvided() throws Exception {
        given(jwtTokenProvider.validateAccessToken("valid-token"))
                .willReturn(new TokenPayload(1L, Role.USER));

        mockMvc.perform(get("/test/protected")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }

    @Test
    void userProfileEndpoint_returns401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.AUTHENTICATION_REQUIRED.code()));
    }

    @Test
    void userProfileEndpoint_returns200_andPassesUserIdFromToken() throws Exception {
        Long expectedUserId = 42L;
        given(jwtTokenProvider.validateAccessToken("valid-token"))
                .willReturn(new TokenPayload(expectedUserId, Role.USER));
        given(getUserProfileUseCase.get(new GetUserProfileUseCase.Query(expectedUserId)))
                .willReturn(new UserProfileResult(
                        expectedUserId, "tester", null, null,
                        Set.of(), Set.of(), Set.of(), Set.of()
                ));

        mockMvc.perform(get("/api/v1/users/me/profile")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(42))
                .andExpect(jsonPath("$.data.nickname").value("tester"));

        verify(getUserProfileUseCase).get(new GetUserProfileUseCase.Query(expectedUserId));
    }


    void authEndpoint_isAccessibleWithoutToken() throws Exception {
        given(googleLoginUseCase.login(any()))
                .willReturn(new AuthTokenResult("access-token", "refresh-token", true));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType("application/json")
                        .content("""
                                { "idToken": "test" }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void kakaoAuthEndpoint_isAccessibleWithoutToken() throws Exception {
        given(kakaoLoginUseCase.login(any()))
                .willReturn(new AuthTokenResult("access-token", "refresh-token", true));

        mockMvc.perform(post("/api/v1/auth/kakao")
                        .contentType("application/json")
                        .content("""
                                { "idToken": "test" }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void refreshEndpoint_isAccessibleWithoutToken() throws Exception {
        given(refreshTokenUseCase.refresh(any()))
                .willReturn(new AuthTokenResult("new-access", "new-refresh", false));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content("""
                                { "refreshToken": "some-token" }
                                """))
                .andExpect(status().isOk());
    }

    @Test

    void swaggerEndpoint_isNotBlockedBySecurityFilter() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @RestController
    static class TestController {

        @GetMapping("/test/protected")
        String protectedEndpoint() {
            return "ok";
        }

        @GetMapping("/v3/api-docs")
        String dummySwaggerEndpoint() {
            return "swagger-dummy";
        }
    }
}
