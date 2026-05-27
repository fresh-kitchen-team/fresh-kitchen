package com.example.freshkitchen.application.alarm.scheduler;

import com.example.freshkitchen.application.alarm.service.ExpiringIngredientNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "fcm.expiring-notification", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExpiringIngredientNotificationScheduler {

    private final ExpiringIngredientNotificationService service;

    @Scheduled(
            cron = "${fcm.expiring-notification.cron}",
            zone = "${fcm.expiring-notification.zone}"
    )
    public void run() {
        try {
            service.notifyExpiring();
        } catch (Exception e) { // SendSummary 반환값은 스케줄러에서 사용하지 않음 (로그만)
            log.error("[ExpiringNotification] scheduler failed", e);
        }
    }
}