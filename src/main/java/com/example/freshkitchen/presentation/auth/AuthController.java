package com.example.freshkitchen.presentation.auth;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.GoogleLoginUseCase;
import com.example.freshkitchen.application.auth.usecase.KakaoLoginUseCase;
import com.example.freshkitchen.application.auth.usecase.LogoutUseCase;
import com.example.freshkitchen.application.auth.usecase.RefreshTokenUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.presentation.auth.dto.AuthRequest;
import com.example.freshkitchen.presentation.auth.dto.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "OAuth 로그인 및 토큰 발급")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final GoogleLoginUseCase googleLoginUseCase;
    private final KakaoLoginUseCase kakaoLoginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    @Operation(summary = "Google 로그인", description = "Google ID Token으로 로그인/회원가입 후 JWT 발급")
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse.Token>> googleLogin(
            @Valid @RequestBody AuthRequest.GoogleLogin request
    ) {
        AuthTokenResult result = googleLoginUseCase.login(
                new GoogleLoginUseCase.Command(request.idToken())
        );
        return ApiResponse.success(AuthResponse.Token.from(result));
    }

    @Operation(summary = "카카오 로그인", description = "Kakao ID Token으로 로그인/회원가입 후 JWT 발급")
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<AuthResponse.Token>> kakaoLogin(
            @Valid @RequestBody AuthRequest.KakaoLogin request
    ) {
        AuthTokenResult result = kakaoLoginUseCase.login(
                new KakaoLoginUseCase.Command(request.idToken())
        );
        return ApiResponse.success(AuthResponse.Token.from(result));
    }

    @Operation(
            summary = "로그아웃",
            description = """
                    Refresh Token을 무효화하고 Access Token을 블랙리스트에 등록하여 세션을 종료합니다.

                    - 호출 후 클라이언트는 반드시 로컬의 accessToken, refreshToken을 삭제해야 합니다.
                    - 이미 로그아웃된 상태에서 재호출해도 200이 반환됩니다 (멱등)."""
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            HttpServletRequest request
    ) {
        String accessToken = extractToken(request);
        logoutUseCase.logout(new LogoutUseCase.Command(userId, accessToken));
        return ApiResponse.success();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새 Access/Refresh Token 발급")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse.TokenRefresh>> refresh(
            @Valid @RequestBody AuthRequest.RefreshToken request
    ) {
        AuthTokenResult result = refreshTokenUseCase.refresh(
                new RefreshTokenUseCase.Command(request.refreshToken())
        );
        return ApiResponse.success(AuthResponse.TokenRefresh.from(result));
    }
}
