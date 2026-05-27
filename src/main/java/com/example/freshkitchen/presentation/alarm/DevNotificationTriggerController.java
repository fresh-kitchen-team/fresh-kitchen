package com.example.freshkitchen.presentation.alarm;

import com.example.freshkitchen.application.alarm.service.ExpiringIngredientNotificationService;
import com.example.freshkitchen.application.alarm.service.ExpiringIngredientNotificationService.SendSummary;
import com.example.freshkitchen.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(name = "dev.notifications.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Dev Notifications", description = "개발 환경 전용 알림 트리거 (운영 비활성화)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dev/notifications")
public class DevNotificationTriggerController {

    private final ExpiringIngredientNotificationService expiringIngredientNotificationService;

    @Operation(summary = "유통기한 임박 알림 즉시 발송", description = "스케줄러 cron을 기다리지 않고 즉시 1회 실행합니다.")
    @PostMapping("/expiring")
    public ResponseEntity<ApiResponse<SendSummary>> triggerExpiring() {
        SendSummary summary = expiringIngredientNotificationService.notifyExpiring();
        return ApiResponse.success(summary);
    }
}
