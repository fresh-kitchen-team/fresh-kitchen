package com.example.freshkitchen.presentation.ingredient.dto;

import com.example.freshkitchen.application.ingredient.usecase.UpdateIngredientUseCase;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record IngredientUpdateRequest(
        Long storageId,
        Long catalogId,
        boolean catalogSet,
        String name,
        LocalDate registeredAt,
        boolean registeredAtSet,
        LocalDate expiresAt,
        boolean expiresAtSet,
        ExpirySourceType expirySourceType,
        String note,
        boolean noteSet,
        IngredientSourceType sourceType
) {

    public static IngredientUpdateRequest from(JsonNode request) {
        if (request == null || !request.isObject()) {
            throw new BusinessValidationException("ingredient update request must be a JSON object");
        }

        return new IngredientUpdateRequest(
                readPositiveLong(request, "storageId"),
                readPositiveLong(request, "catalogId"),
                request.has("catalogId"),
                readString(request, "name"),
                readDate(request, "registeredAt"),
                request.has("registeredAt"),
                readDate(request, "expiresAt"),
                request.has("expiresAt"),
                readEnum(request, "expirySourceType", ExpirySourceType.class),
                readString(request, "note"),
                request.has("note"),
                readEnum(request, "sourceType", IngredientSourceType.class)
        );
    }

    public UpdateIngredientUseCase.Command toCommand(Long ingredientId, Long userId) {
        return new UpdateIngredientUseCase.Command(
                ingredientId,
                userId,
                storageId,
                catalogId,
                catalogSet,
                name,
                registeredAt,
                registeredAtSet,
                expiresAt,
                expiresAtSet,
                expirySourceType,
                note,
                noteSet,
                sourceType
        );
    }

    private static Long readPositiveLong(JsonNode request, String fieldName) {
        JsonNode value = request.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.canConvertToLong()) {
            throw new BusinessValidationException(fieldName + " must be a number");
        }
        long resolvedValue = value.longValue();
        if (resolvedValue <= 0) {
            throw new BusinessValidationException(fieldName + " must be positive");
        }
        return resolvedValue;
    }

    private static String readString(JsonNode request, String fieldName) {
        JsonNode value = request.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new BusinessValidationException(fieldName + " must be a string");
        }
        return value.textValue();
    }

    private static LocalDate readDate(JsonNode request, String fieldName) {
        String value = readString(request, fieldName);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessValidationException(fieldName + " must be date", exception);
        }
    }

    private static <T extends Enum<T>> T readEnum(JsonNode request, String fieldName, Class<T> type) {
        String value = readString(request, fieldName);
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException(fieldName + " contains invalid value", exception);
        }
    }
}
