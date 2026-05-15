package com.example.freshkitchen.presentation.item.dto;

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

public final class ItemRequest {

    private static final int NAME_MAX_LENGTH = 100;

    private ItemRequest() {
    }

    public record Create(
            @NotBlank @Size(max = NAME_MAX_LENGTH) String name,
            @NotNull @Positive Long catalogId,
            @NotNull @Positive Long storageId,
            LocalDate expiryDate,
            LocalDate purchaseDate,
            String memo,
            @Positive Long imageAssetId
    ) {

        public CreateIngredientUseCase.Command toCommand(Long userId, LocalDate defaultPurchaseDate) {
            return new CreateIngredientUseCase.Command(
                    userId,
                    storageId,
                    catalogId,
                    name,
                    purchaseDate != null ? purchaseDate : defaultPurchaseDate,
                    expiryDate,
                    ExpirySourceType.MANUAL,
                    memo,
                    imageAssetId != null ? IngredientSourceType.PHOTO : IngredientSourceType.MANUAL
            );
        }
    }

    public record Update(
            String name,
            Long catalogId,
            boolean catalogSet,
            Long storageId,
            LocalDate expiryDate,
            boolean expiryDateSet,
            LocalDate purchaseDate,
            boolean purchaseDateSet,
            String memo,
            boolean memoSet
    ) {

        public static Update from(JsonNode request) {
            if (request == null || !request.isObject()) {
                throw new BusinessValidationException("item update request must be a JSON object");
            }

            rejectExplicitNull(request, "name");
            rejectExplicitNull(request, "catalogId");
            rejectExplicitNull(request, "storageId");

            return new Update(
                    readBoundedString(request, "name", NAME_MAX_LENGTH),
                    readPositiveLong(request, "catalogId"),
                    request.has("catalogId"),
                    readPositiveLong(request, "storageId"),
                    readDate(request, "expiryDate"),
                    request.has("expiryDate"),
                    readDate(request, "purchaseDate"),
                    request.has("purchaseDate"),
                    readString(request, "memo"),
                    request.has("memo")
            );
        }

        public UpdateIngredientUseCase.Command toCommand(Long itemId, Long userId) {
            return new UpdateIngredientUseCase.Command(
                    itemId,
                    userId,
                    storageId,
                    catalogId,
                    catalogSet,
                    name,
                    purchaseDate,
                    purchaseDateSet,
                    expiryDate,
                    expiryDateSet,
                    null,
                    memo,
                    memoSet,
                    null
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
        if (value != null && value.isBlank()) {
            throw new BusinessValidationException(fieldName + " must not be blank");
        }
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
}
