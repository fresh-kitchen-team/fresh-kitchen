package com.example.freshkitchen.global.config;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.GoogleLoginUseCase;
import com.example.freshkitchen.application.auth.usecase.KakaoLoginUseCase;
import com.example.freshkitchen.application.auth.usecase.DevLoginUseCase;
import com.example.freshkitchen.application.auth.usecase.RefreshTokenUseCase;
import com.example.freshkitchen.application.inquiry.usecase.SendInquiryUseCase;
import com.example.freshkitchen.application.legal.usecase.AgreeTermsUseCase;
import com.example.freshkitchen.application.legal.usecase.GetTermsAgreementUseCase;
import com.example.freshkitchen.application.chat.usecase.CreateChatRoomUseCase;
import com.example.freshkitchen.application.chat.usecase.GetChatHistoryUseCase;
import com.example.freshkitchen.application.chat.usecase.GetChatRoomListUseCase;
import com.example.freshkitchen.application.chat.usecase.SendChatMessageUseCase;
import com.example.freshkitchen.application.chat.usecase.UpdateChatRoomTitleUseCase;
import com.example.freshkitchen.application.home.usecase.GetHomeSummaryUseCase;
import com.example.freshkitchen.application.image.usecase.ChangeIngredientPrimaryImageUseCase;
import com.example.freshkitchen.application.image.usecase.UploadIngredientImageUseCase;
import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientWithImageUseCase;
import com.example.freshkitchen.application.ingredient.usecase.DeleteIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.GetIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ListIngredientsUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ListStoragesUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ResolveIngredientDefaultsUseCase;
import com.example.freshkitchen.application.ingredient.usecase.UpdateIngredientUseCase;
import com.example.freshkitchen.application.scan.usecase.ScanIngredientImageUseCase;
import com.example.freshkitchen.application.scan.usecase.ScanReceiptImageUseCase;
import com.example.freshkitchen.application.user.dto.UserProfileResult;
import com.example.freshkitchen.application.user.usecase.DeleteUserProfileUseCase;
import com.example.freshkitchen.application.user.usecase.GetUserProfileUseCase;
import com.example.freshkitchen.application.user.usecase.UpdateUserProfileUseCase;
import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import com.example.freshkitchen.global.security.exception.JwtTokenException;
import com.example.freshkitchen.global.security.exception.SecurityErrorCode;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import com.example.freshkitchen.global.security.infrastructure.TokenPayload;
import com.example.freshkitchen.domain.chat.serivce.ChatService;
import com.example.freshkitchen.infrastructure.ai.AiServerClient;
import com.example.freshkitchen.infrastructure.app.AppVersionProperties;
import com.example.freshkitchen.infrastructure.image.LocalImageStorageProperties;
import com.example.freshkitchen.infrastructure.inquiry.InquiryProperties;
import com.example.freshkitchen.infrastructure.legal.LegalProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({
        SecurityConfig.class,
        LocalImageStorageProperties.class,
        SecurityFilterChainIntegrationTest.TestController.class
})
@TestPropertySource(properties = "image.storage.local.public-base-url=/assets")
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
    private CreateIngredientWithImageUseCase createIngredientWithImageUseCase;

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
    private DeleteIngredientUseCase deleteIngredientUseCase;

    @MockitoBean
    private UploadIngredientImageUseCase uploadIngredientImageUseCase;

    @MockitoBean
    private ChangeIngredientPrimaryImageUseCase changeIngredientPrimaryImageUseCase;

    @MockitoBean
    private ScanIngredientImageUseCase scanIngredientImageUseCase;

    @MockitoBean
    private ScanReceiptImageUseCase scanReceiptImageUseCase;

    @MockitoBean
    private GoogleLoginUseCase googleLoginUseCase;

    @MockitoBean
    private KakaoLoginUseCase kakaoLoginUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockitoBean
    private DevLoginUseCase devLoginUseCase;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private AiServerClient aiServerClient;

    @MockitoBean
    private Clock clock;

    @MockitoBean
    private CreateChatRoomUseCase createChatRoomUseCase;

    @MockitoBean
    private GetChatRoomListUseCase getChatRoomListUseCase;

    @MockitoBean
    private GetChatHistoryUseCase getChatHistoryUseCase;

    @MockitoBean
    private SendChatMessageUseCase sendChatMessageUseCase;

    @MockitoBean
    private UpdateChatRoomTitleUseCase updateChatRoomTitleUseCase;

    @MockitoBean
    private SendInquiryUseCase sendInquiryUseCase;

    @MockitoBean
    private AgreeTermsUseCase agreeTermsUseCase;

    @MockitoBean
    private GetTermsAgreementUseCase getTermsAgreementUseCase;

    @MockitoBean
    private AppVersionProperties appVersionProperties;

    @MockitoBean
    private LegalProperties legalProperties;

    @MockitoBean
    private InquiryProperties inquiryProperties;

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
    void ingredientImageEndpoint_returns401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(multipart("/api/v1/ingredients/1/images"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.AUTHENTICATION_REQUIRED.code()));
    }

    @Test
    void itemEndpoint_returns401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.AUTHENTICATION_REQUIRED.code()));
    }

    @Test
    void primaryIngredientImageEndpoint_returns401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(patch("/api/v1/ingredients/1/images/primary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.AUTHENTICATION_REQUIRED.code()));
    }

    @Test
    void scanIngredientImageEndpoint_returns401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(multipart("/api/v1/scan/ingredient-image"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.AUTHENTICATION_REQUIRED.code()));
    }

    @Test
    void scanReceiptImageEndpoint_returns401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(multipart("/api/v1/scan/receipt-image"))
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

    @Test
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

    @Test
    void appVersionEndpoint_isAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/app/version"))
                .andExpect(status().isOk());
    }

    @Test
    void legalEndpoint_isAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/legal"))
                .andExpect(status().isOk());
    }

    @Test
    void legalAgreementPostEndpoint_returns401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(post("/api/v1/legal/agreement"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.AUTHENTICATION_REQUIRED.code()));
    }

    @Test
    void legalAgreementGetEndpoint_returns401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/legal/agreement"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.AUTHENTICATION_REQUIRED.code()));
    }

    @Test
    void inquiryEndpoint_returns401_whenNoTokenProvided() throws Exception {
        mockMvc.perform(multipart("/api/v1/inquiries"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(SecurityErrorCode.AUTHENTICATION_REQUIRED.code()));
    }

    @Test
    void configuredUploadEndpoint_isAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/assets/images/test.png"))
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

        @GetMapping("/assets/images/test.png")
        String dummyUploadEndpoint() {
            return "upload-dummy";
        }
    }
}
