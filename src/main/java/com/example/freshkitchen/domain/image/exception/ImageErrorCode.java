package com.example.freshkitchen.domain.image.exception;

import com.example.freshkitchen.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ImageErrorCode implements ErrorCode {

    INGREDIENT_IMAGE_ALREADY_ATTACHED(HttpStatus.BAD_REQUEST, "IMAGE-400-1", "ingredient image is already attached to another ingredient"),
    SYSTEM_DEFAULT_OWNER_MUST_BE_NULL(HttpStatus.BAD_REQUEST, "IMAGE-400-2", "user must be null when assetType is SYSTEM_DEFAULT"),
    USER_UPLOAD_OWNER_REQUIRED(HttpStatus.BAD_REQUEST, "IMAGE-400-3", "user must not be null when assetType is USER_UPLOAD");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ImageErrorCode(HttpStatus status, String code, String message) {
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
