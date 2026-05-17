package com.example.freshkitchen.infrastructure.app;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.version")
public class AppVersionProperties {

    private String latestVersion;
    private String minimumVersion;
    private boolean forceUpdate;
    private String updateUrl;
}
