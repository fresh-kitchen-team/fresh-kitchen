package com.example.freshkitchen.presentation.ingredient.dto;

import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.UpdateIngredientUseCase;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class IngredientRequest {

    private static final int NAME_MAX_LENGTH = 100;

    private IngredientRequest() {
    }

    public record Create(
            @NotNull @Positive Long storageId,
            @Positive Long catalogId,
            @NotBlank @Size(max = NAME_MAX_LENGTH) String name,
            LocalDate registeredAt,
            LocalDate expiresAt,
            @NotNull ExpirySourceType expirySourceType,
            String note,
            @NotNull IngredientSourceType sourceType
    ) {

        public CreateIngredientUseCase.Command toCommand(Long userId) {
            return new CreateIngredientUseCase.Command(
                    userId,
                    storageId,
                    catalogId,
                    name,
                    registeredAt,
                    expiresAt,
                    expirySourceType,
                    note,
                    sourceType
            );
        }
    }

    public record Update(
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

        public static Update from(JsonNode request) {
            if (request == null || !request.isObject()) {
                throw new BusinessValidationException("ingredient update request must be a JSON object");
            }

            rejectExplicitNull(request, "storageId");
            rejectExplicitNull(request, "name");
            rejectExplicitNull(request, "expirySourceType");
            rejectExplicitNull(request, "sourceType");

            return new Update(
                    readPositiveLong(request, "storageId"),
                    readPositiveLong(request, "catalogId"),
                    request.has("catalogId"),
                    readBoundedString(request, "name", NAME_MAX_LENGTH),
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
    }

    private static void rejectExplicitNull(JsonNode request, String fieldName) {
        if (request.has(fieldName) && request.get(fieldName).isNull()) {
            throw new BusinessValidationException(fieldName + " must not be null");
        }
    }

    private static Long readPositiveLong(JsonNode request, String fieldName) {
        JsonNode value = request.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToLong()) {
            throw new BusinessValidationException(fieldName + " must be an integer");
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

    private static String readBoundedString(JsonNode request, String fieldName, int maxLength) {
        String value = readString(request, fieldName);
        if (value != null && value.length() > maxLength) {
            throw new BusinessValidationException(fieldName + " must be at most " + maxLength + " characters");
        }
        return value;
    }

    private static LocalDate readDate(JsonNode request, String fieldName) {
        String value = readString(request, fieldName);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessValidationException(fieldName + " must be a valid date", exception);
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
