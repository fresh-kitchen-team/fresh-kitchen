package com.example.freshkitchen.presentation.auth;

import com.example.freshkitchen.application.user.usecase.HardDeleteUserUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dev Auth", description = "개발 환경 전용 인증 API (운영 비활성화)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class DevHardDeleteController {

    private final HardDeleteUserUseCase hardDeleteUserUseCase;

    @Operation(
            summary = "개발용 계정 완전 삭제",
            description = "인증된 사용자의 계정과 모든 연관 데이터를 완전히 삭제합니다. dev-login으로 토큰을 발급받은 뒤 호출하세요."
    )
    @DeleteMapping("/dev-hard-delete")
    public ResponseEntity<ApiResponse<Void>> hardDeleteUser(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        hardDeleteUserUseCase.delete(new HardDeleteUserUseCase.Command(userId));
        return ApiResponse.success();
    }
}
