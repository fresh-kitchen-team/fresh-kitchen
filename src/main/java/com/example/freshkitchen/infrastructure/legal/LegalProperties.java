package com.example.freshkitchen.infrastructure.legal;

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
@ConfigurationProperties(prefix = "app.legal")
public class LegalProperties {

    @NotBlank
    private String termsUrl;

    @NotBlank
    private String privacyUrl;
}
