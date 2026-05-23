package com.example.freshkitchen.application.alarm.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "fcm.expiring-notification")
public class ExpiringIngredientNotificationProperties {

    private boolean enabled = true;

    @NotBlank
    private String cron = "0 0 9 * * *";

    @NotBlank
    private String zone = "Asia/Seoul";

    @Min(0)
    private int daysAhead = 1;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public int getDaysAhead() {
        return daysAhead;
    }

    public void setDaysAhead(int daysAhead) {
        this.daysAhead = daysAhead;
    }
}
