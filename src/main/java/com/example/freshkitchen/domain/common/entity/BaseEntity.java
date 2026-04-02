package com.example.freshkitchen.domain.common.entity;

import jakarta.persistence.MappedSuperclass;

import java.util.LinkedHashSet;
import java.util.Set;

@MappedSuperclass
public abstract class BaseEntity {

    protected static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    protected static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    protected static int requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    protected static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return value;
    }

    protected static <T> LinkedHashSet<T> toLinkedHashSet(Set<T> values) {
        return values == null ? new LinkedHashSet<>() : new LinkedHashSet<>(values);
    }
}
