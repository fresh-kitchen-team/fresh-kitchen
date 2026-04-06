package com.example.freshkitchen.domain.ingredient.exception;

import com.example.freshkitchen.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum IngredientErrorCode implements ErrorCode {

    INGREDIENT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "INGREDIENT-400-1", "ingredientId must not be null"),
    INGREDIENT_IMAGE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "INGREDIENT-400-2", "ingredientImageId must not be null"),
    INGREDIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "INGREDIENT-404-1", "ingredient not found"),
    INGREDIENT_IMAGE_NOT_BELONG_TO_INGREDIENT(HttpStatus.BAD_REQUEST, "INGREDIENT-400-3", "ingredient image must belong to ingredient"),
    FIRST_IMAGE_MUST_BE_PRIMARY(HttpStatus.BAD_REQUEST, "INGREDIENT-400-4", "first ingredient image must be primary"),
    PRIMARY_IMAGE_MUST_BELONG_TO_INGREDIENT(HttpStatus.BAD_REQUEST, "INGREDIENT-400-5", "primary image must belong to ingredient"),
    INGREDIENT_PRIMARY_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "INGREDIENT-400-6", "ingredient must have one primary image"),
    INGREDIENT_PRIMARY_IMAGE_INVARIANT_BROKEN(HttpStatus.CONFLICT, "INGREDIENT-409-1", "ingredient must have exactly one primary image"),
    STORAGE_NOT_OWNED_BY_USER(HttpStatus.BAD_REQUEST, "INGREDIENT-400-7", "storage must belong to user");

    private final HttpStatus status;
    private final String code;
    private final String message;

    IngredientErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
