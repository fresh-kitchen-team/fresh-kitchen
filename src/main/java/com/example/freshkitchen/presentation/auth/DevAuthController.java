package com.example.freshkitchen.presentation.auth;

import com.example.freshkitchen.application.auth.usecase.DevLoginUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.presentation.auth.dto.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@Tag(name = "Dev Auth", description = "개발 환경 전용 로그인 (운영 비활성화)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class DevAuthController {

    private final DevLoginUseCase devLoginUseCase;

    @Operation(
            summary = "개발용 로그인",
            description = "OAuth 없이 userId로 즉시 로그인합니다. 7일 유효 토큰이 발급됩니다. dev 프로필에서만 동작합니다."
    )
    @PostMapping("/dev-login")
    public ResponseEntity<ApiResponse<AuthResponse.Token>> devLogin(
            @Valid @RequestBody DevLoginRequest request
    ) {
        AuthResponse.Token response = AuthResponse.Token.from(
                devLoginUseCase.login(new DevLoginUseCase.Command(request.userId()))
        );
        return ApiResponse.success(response);
    }

    public record DevLoginRequest(
            @NotNull @Positive Long userId
    ) {
    }
}
