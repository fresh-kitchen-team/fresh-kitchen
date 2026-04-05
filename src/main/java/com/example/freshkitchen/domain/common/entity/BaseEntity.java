package com.example.freshkitchen.domain.common.entity;

import jakarta.persistence.MappedSuperclass;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

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

    protected static Integer requirePositiveNullable(Integer value, String fieldName) {
        return value == null ? null : requirePositive(value, fieldName);
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

    protected static <T, ID> boolean sameEntity(T left, T right, Function<T, ID> idExtractor) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }

        ID leftId = idExtractor.apply(left);
        ID rightId = idExtractor.apply(right);
        return leftId != null && rightId != null && leftId.equals(rightId);
    }
}
