package com.example.freshkitchen.domain.image.enums;

import com.example.freshkitchen.global.exception.BusinessValidationException;

import java.util.Arrays;
import java.util.Locale;

public enum ImageContentType {

    JPEG("image/jpeg", ".jpg"),
    PNG("image/png", ".png"),
    WEBP("image/webp", ".webp");

    private final String value;
    private final String extension;

    ImageContentType(String value, String extension) {
        this.value = value;
        this.extension = extension;
    }

    public static ImageContentType from(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessValidationException("contentType must not be blank");
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.value.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new BusinessValidationException("contentType must be supported image type"));
    }

    public String value() {
        return value;
    }

    public String extension() {
        return extension;
    }
}
