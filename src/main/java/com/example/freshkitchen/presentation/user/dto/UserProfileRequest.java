package com.example.freshkitchen.presentation.user.dto;

import com.example.freshkitchen.application.user.usecase.UpdateUserProfileUseCase;
import com.example.freshkitchen.domain.user.enums.AllergyType;
import com.example.freshkitchen.domain.user.enums.CookingTool;
import com.example.freshkitchen.domain.user.enums.FoodStyle;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.Set;

public final class UserProfileRequest {

    private UserProfileRequest() {
    }

    public record Update(
            JsonNode body
    ) {

        public UpdateUserProfileUseCase.Command toCommand(Long userId) {
            JsonNode object = requireObjectBody();
            return new UpdateUserProfileUseCase.Command(
                    userId,
                    nullableText(object, "nickname"),
                    nullableText(object, "profileImageUrl"),
                    object.has("profileImageUrl"),
                    nullableText(object, "bio"),
                    object.has("bio"),
                    stringSet(object, "preferredIngredients"),
                    enumSet(object, "foodStyles", FoodStyle.class),
                    enumSet(object, "allergies", AllergyType.class),
                    enumSet(object, "cookingTools", CookingTool.class)
            );
        }

        private JsonNode requireObjectBody() {
            if (body == null || body.isNull() || !body.isObject()) {
                throw new BusinessValidationException("request body must be object");
            }
            return body;
        }
    }

    private static String nullableText(JsonNode object, String fieldName) {
        JsonNode value = object.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isString()) {
            throw new BusinessValidationException(fieldName + " must be text");
        }
        return value.stringValue();
    }

    private static Set<String> stringSet(JsonNode object, String fieldName) {
        JsonNode value = object.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isArray()) {
            throw new BusinessValidationException(fieldName + " must be array");
        }

        Set<String> values = new LinkedHashSet<>();
        value.forEach(item -> {
            if (!item.isString()) {
                throw new BusinessValidationException(fieldName + " must contain text");
            }
            values.add(item.stringValue());
        });
        return values;
    }

    private static <E extends Enum<E>> Set<E> enumSet(
            JsonNode object,
            String fieldName,
            Class<E> enumType
    ) {
        JsonNode value = object.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isArray()) {
            throw new BusinessValidationException(fieldName + " must be array");
        }

        Set<E> values = new LinkedHashSet<>();
        value.forEach(item -> values.add(enumValue(fieldName, enumType, item)));
        return values;
    }

    private static <E extends Enum<E>> E enumValue(
            String fieldName,
            Class<E> enumType,
            JsonNode item
    ) {
        if (!item.isString()) {
            throw new BusinessValidationException(fieldName + " must contain text");
        }
        try {
            return Enum.valueOf(enumType, item.stringValue());
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException(fieldName + " contains invalid value");
        }
    }
}
