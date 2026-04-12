package com.example.freshkitchen.global.exception;

import com.example.freshkitchen.domain.image.exception.ImageErrorCode;
import com.example.freshkitchen.domain.image.exception.ImageException;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorCodeContractTest {

    @Test
    void commonErrorCode_contractMatchesSpecification() {
        assertContract(CommonErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST, "COMMON-400", "Invalid input");
        assertContract(CommonErrorCode.INVALID_STATE, HttpStatus.CONFLICT, "COMMON-409", "Invalid state");
        assertContract(
                CommonErrorCode.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "COMMON-500",
                "Internal server error"
        );
    }

    @Test
    void ingredientErrorCode_contractMatchesSpecification() {
        assertContract(
                IngredientErrorCode.INGREDIENT_ID_REQUIRED,
                HttpStatus.BAD_REQUEST,
                "INGREDIENT-400-1",
                "ingredientId must not be null"
        );
        assertContract(
                IngredientErrorCode.INGREDIENT_IMAGE_ID_REQUIRED,
                HttpStatus.BAD_REQUEST,
                "INGREDIENT-400-2",
                "ingredientImageId must not be null"
        );
        assertContract(
                IngredientErrorCode.INGREDIENT_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "INGREDIENT-404-1",
                "ingredient not found"
        );
        assertContract(
                IngredientErrorCode.INGREDIENT_IMAGE_NOT_BELONG_TO_INGREDIENT,
                HttpStatus.BAD_REQUEST,
                "INGREDIENT-400-3",
                "ingredient image must belong to ingredient"
        );
        assertContract(
                IngredientErrorCode.FIRST_IMAGE_MUST_BE_PRIMARY,
                HttpStatus.BAD_REQUEST,
                "INGREDIENT-400-4",
                "first ingredient image must be primary"
        );
        assertContract(
                IngredientErrorCode.PRIMARY_IMAGE_MUST_BELONG_TO_INGREDIENT,
                HttpStatus.BAD_REQUEST,
                "INGREDIENT-400-5",
                "primary image must belong to ingredient"
        );
        assertContract(
                IngredientErrorCode.INGREDIENT_PRIMARY_IMAGE_REQUIRED,
                HttpStatus.BAD_REQUEST,
                "INGREDIENT-400-6",
                "ingredient must have one primary image"
        );
        assertContract(
                IngredientErrorCode.INGREDIENT_PRIMARY_IMAGE_INVARIANT_BROKEN,
                HttpStatus.CONFLICT,
                "INGREDIENT-409-1",
                "ingredient must have exactly one primary image"
        );
        assertContract(
                IngredientErrorCode.STORAGE_NOT_OWNED_BY_USER,
                HttpStatus.BAD_REQUEST,
                "INGREDIENT-400-7",
                "storage must belong to user"
        );
    }

    @Test
    void imageErrorCode_contractMatchesSpecification() {
        assertContract(
                ImageErrorCode.INGREDIENT_IMAGE_ALREADY_ATTACHED,
                HttpStatus.BAD_REQUEST,
                "IMAGE-400-1",
                "ingredient image is already attached to another ingredient"
        );
        assertContract(
                ImageErrorCode.SYSTEM_DEFAULT_OWNER_MUST_BE_NULL,
                HttpStatus.BAD_REQUEST,
                "IMAGE-400-2",
                "user must be null when assetType is SYSTEM_DEFAULT"
        );
        assertContract(
                ImageErrorCode.USER_UPLOAD_OWNER_REQUIRED,
                HttpStatus.BAD_REQUEST,
                "IMAGE-400-3",
                "user must not be null when assetType is USER_UPLOAD"
        );
    }

    @Test
    void domainExceptions_exposeTheirErrorCodes() {
        IngredientException ingredientException = new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND);
        ImageException imageException = new ImageException(ImageErrorCode.USER_UPLOAD_OWNER_REQUIRED);
        BusinessValidationException validationException = new BusinessValidationException("name must not be blank");

        assertEquals(IngredientErrorCode.INGREDIENT_NOT_FOUND, ingredientException.getErrorCode());
        assertEquals(ImageErrorCode.USER_UPLOAD_OWNER_REQUIRED, imageException.getErrorCode());
        assertEquals(CommonErrorCode.INVALID_INPUT, validationException.getErrorCode());
    }

    private static void assertContract(ErrorCode errorCode, HttpStatus status, String code, String message) {
        assertEquals(status, errorCode.status());
        assertEquals(code, errorCode.code());
        assertEquals(message, errorCode.message());
    }
}
