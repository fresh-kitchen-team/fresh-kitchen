package com.example.freshkitchen.infrastructure.app;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "app.version")
public class AppVersionProperties {

    @NotBlank
    private String latestVersion;

    @NotBlank
    private String minimumVersion;

    private boolean forceUpdate;
    private String updateUrl;
}
