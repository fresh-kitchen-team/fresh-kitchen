package com.example.freshkitchen.infrastructure.image;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LocalImageStoragePropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validation_acceptsAbsolutePathPublicBaseUrl() {
        LocalImageStorageProperties properties = properties("/uploads");

        Set<ConstraintViolation<LocalImageStorageProperties>> violations = validator.validate(properties);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_rejectsBlankPublicBaseUrl() {
        LocalImageStorageProperties properties = properties("");

        Set<ConstraintViolation<LocalImageStorageProperties>> violations = validator.validate(properties);

        assertFalse(violations.isEmpty());
    }

    @Test
    void validation_rejectsUrlPublicBaseUrl() {
        LocalImageStorageProperties properties = properties("https://cdn.example.com");

        Set<ConstraintViolation<LocalImageStorageProperties>> violations = validator.validate(properties);

        assertFalse(violations.isEmpty());
    }

    @Test
    void validation_rejectsRelativePublicBaseUrl() {
        LocalImageStorageProperties properties = properties("uploads");

        Set<ConstraintViolation<LocalImageStorageProperties>> violations = validator.validate(properties);

        assertFalse(violations.isEmpty());
    }

    @Test
    void validation_rejectsRootPublicBaseUrl() {
        LocalImageStorageProperties properties = properties("/");

        Set<ConstraintViolation<LocalImageStorageProperties>> violations = validator.validate(properties);

        assertFalse(violations.isEmpty());
    }

    @Test
    void publicResourcePattern_normalizesTrailingSlash() {
        LocalImageStorageProperties properties = properties("/uploads/");

        assertEquals("/uploads/**", properties.publicResourcePattern());
    }

    private static LocalImageStorageProperties properties(String publicBaseUrl) {
        LocalImageStorageProperties properties = new LocalImageStorageProperties();
        properties.setRootDir("uploads");
        properties.setPublicBaseUrl(publicBaseUrl);
        return properties;
    }
}
