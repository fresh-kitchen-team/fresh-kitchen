package com.example.freshkitchen.domain.chat.intent;

import com.example.freshkitchen.presentation.chat.dto.request.ChatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "chat.intent-classifier")
public class IntentClassifierProperties {

    private boolean enabled = true;

    @NotBlank
    private String model = "gemini-2.5-flash-lite";

    @NotNull
    private ChatType fallbackType = ChatType.GENERAL;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public ChatType getFallbackType() {
        return fallbackType;
    }

    public void setFallbackType(ChatType fallbackType) {
        this.fallbackType = fallbackType;
    }
}