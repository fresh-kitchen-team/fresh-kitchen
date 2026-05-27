package com.example.freshkitchen.presentation.user;

import com.example.freshkitchen.application.user.usecase.RegisterFcmTokenUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.presentation.user.dto.FcmTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User FCM Token", description = "푸시 알림을 위한 FCM 디바이스 토큰 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/fcm-tokens")
public class FcmTokenController {

    private final RegisterFcmTokenUseCase registerFcmTokenUseCase;

    @Operation(summary = "FCM 디바이스 토큰 등록/갱신")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> register(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FcmTokenRequest.Register request
    ) {
        registerFcmTokenUseCase.register(request.toCommand(userId));
        return ApiResponse.success(HttpStatus.CREATED, (Void) null);
    }
}
