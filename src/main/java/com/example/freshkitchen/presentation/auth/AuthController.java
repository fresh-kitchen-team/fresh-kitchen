package com.example.freshkitchen.presentation.auth;

import com.example.freshkitchen.application.auth.dto.AuthTokenResult;
import com.example.freshkitchen.application.auth.usecase.GoogleLoginUseCase;
import com.example.freshkitchen.application.auth.usecase.KakaoLoginUseCase;
import com.example.freshkitchen.application.auth.usecase.RefreshTokenUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.presentation.auth.dto.AuthRequest;
import com.example.freshkitchen.presentation.auth.dto.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
