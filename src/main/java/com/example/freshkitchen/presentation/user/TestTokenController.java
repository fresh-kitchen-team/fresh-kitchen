package com.example.freshkitchen.presentation.user;

import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.global.security.infrastructure.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local") // 로컬 환경에서만 확인용 (임시)
@Tag(name = "Test API (Local Only)", description = "로컬 스웨거 테스트용 임시 API (운영 환경 노출 X)")
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestTokenController { // <Swagger Authorize에 토큰 입력 -> 파라미터 입력 확인> 이거 테스트용

    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "임시 Access Token 발급 (Swagger Authorize 테스트용)")
    @GetMapping("/token/{userId}")
    public String getTestToken(@PathVariable Long userId) {
        return jwtTokenProvider.generateAccessToken(userId, Role.USER);
    }
}