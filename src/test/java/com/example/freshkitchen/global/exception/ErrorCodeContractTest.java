package com.example.freshkitchen.global.exception;

import com.example.freshkitchen.domain.image.exception.ImageErrorCode;
import com.example.freshkitchen.domain.image.exception.ImageException;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import com.example.freshkitchen.global.security.exception.JwtTokenException;
import com.example.freshkitchen.global.security.exception.OAuthErrorCode;
import com.example.freshkitchen.global.security.exception.OAuthException;
import com.example.freshkitchen.global.security.exception.SecurityErrorCode;
import com.example.freshkitchen.infrastructure.ai.exception.AiServerErrorCode;
import com.example.freshkitchen.infrastructure.ai.exception.AiServerException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
        assertContract(
                IngredientErrorCode.STORAGE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "INGREDIENT-404-2",
                "storage not found"
        );
        assertContract(
                IngredientErrorCode.CATALOG_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "INGREDIENT-404-3",
                "ingredient catalog not found"
        );
        assertContract(
                IngredientErrorCode.USER_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "INGREDIENT-404-4",
                "user not found"
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
    void userErrorCode_contractMatchesSpecification() {
        assertContract(
                UserErrorCode.USER_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "USER-404-1",
                "user not found"
        );
    }

    @Test
    void securityErrorCode_contractMatchesSpecification() {
        assertContract(SecurityErrorCode.AUTHENTICATION_REQUIRED, HttpStatus.UNAUTHORIZED, "AUTH-401-0", "authentication required");
    }

    @Test
    void oAuthErrorCode_contractMatchesSpecification() {
        assertContract(OAuthErrorCode.INVALID_ID_TOKEN, HttpStatus.UNAUTHORIZED, "AUTH-401-7", "invalid id token");
        assertContract(OAuthErrorCode.PROVIDER_NOT_SUPPORTED, HttpStatus.BAD_REQUEST, "AUTH-400-1", "oauth provider not supported");
    }

    @Test
    void jwtErrorCode_contractMatchesSpecification() {
        assertContract(JwtErrorCode.EXPIRED_TOKEN, HttpStatus.UNAUTHORIZED, "AUTH-401-1", "expired token");
        assertContract(JwtErrorCode.INVALID_SIGNATURE, HttpStatus.UNAUTHORIZED, "AUTH-401-2", "invalid token signature");
        assertContract(JwtErrorCode.MALFORMED_TOKEN, HttpStatus.UNAUTHORIZED, "AUTH-401-3", "malformed token");
        assertContract(JwtErrorCode.UNSUPPORTED_TOKEN, HttpStatus.UNAUTHORIZED, "AUTH-401-4", "unsupported token");
        assertContract(JwtErrorCode.EMPTY_CLAIMS, HttpStatus.UNAUTHORIZED, "AUTH-401-5", "token claims are empty");
        assertContract(JwtErrorCode.NOT_YET_VALID_TOKEN, HttpStatus.UNAUTHORIZED, "AUTH-401-6", "token is not yet valid");
    }

    @Test
    void aiServerErrorCode_contractMatchesSpecification() {
        assertContract(
                AiServerErrorCode.AI_SERVER_UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI-503-1",
                "AI server unavailable"
        );
        assertContract(
                AiServerErrorCode.AI_SERVER_TIMEOUT,
                HttpStatus.GATEWAY_TIMEOUT,
                "AI-504-1",
                "AI server timeout"
        );
        assertContract(
                AiServerErrorCode.AI_RESPONSE_INVALID,
                HttpStatus.BAD_GATEWAY,
                "AI-502-1",
                "AI response invalid"
        );
    }

    @Test
    void domainExceptions_exposeTheirErrorCodes() {
        IngredientException ingredientException = new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND);
        ImageException imageException = new ImageException(ImageErrorCode.USER_UPLOAD_OWNER_REQUIRED);
        UserException userException = new UserException(UserErrorCode.USER_NOT_FOUND);
        AiServerException aiServerException = new AiServerException(AiServerErrorCode.AI_SERVER_UNAVAILABLE);
        BusinessValidationException validationException = new BusinessValidationException("name must not be blank");

        assertEquals(IngredientErrorCode.INGREDIENT_NOT_FOUND, ingredientException.getErrorCode());
        assertEquals(ImageErrorCode.USER_UPLOAD_OWNER_REQUIRED, imageException.getErrorCode());
        assertEquals(UserErrorCode.USER_NOT_FOUND, userException.getErrorCode());
        assertEquals(AiServerErrorCode.AI_SERVER_UNAVAILABLE, aiServerException.getErrorCode());
        assertEquals(CommonErrorCode.INVALID_INPUT, validationException.getErrorCode());
    }

    @Test
    void jwtTokenException_exposesItsErrorCode() {
        JwtTokenException jwtException = new JwtTokenException(JwtErrorCode.EXPIRED_TOKEN);

        assertEquals(JwtErrorCode.EXPIRED_TOKEN, jwtException.getErrorCode());
    }

    @Test
    void oAuthException_exposesItsErrorCode() {
        OAuthException oAuthException = new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN);

        assertEquals(OAuthErrorCode.INVALID_ID_TOKEN, oAuthException.getErrorCode());
    }

    @Test
    void oAuthException_preservesCause() {
        RuntimeException cause = new RuntimeException("token verification failed");

        OAuthException exception = new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN, cause);

        assertSame(cause, exception.getCause());
    }

    @Test
    void aiServerException_preservesCause() {
        RuntimeException cause = new RuntimeException("connection refused");

        AiServerException exception = new AiServerException(AiServerErrorCode.AI_SERVER_UNAVAILABLE, cause);

        assertSame(cause, exception.getCause());
    }

    private static void assertContract(ErrorCode errorCode, HttpStatus status, String code, String message) {
        assertEquals(status, errorCode.status());
        assertEquals(code, errorCode.code());
        assertEquals(message, errorCode.message());
    }
}
