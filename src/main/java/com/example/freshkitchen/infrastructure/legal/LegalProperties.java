package com.example.freshkitchen.infrastructure.legal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.legal")
public class LegalProperties {

    private String termsUrl;
    private String privacyUrl;
}
