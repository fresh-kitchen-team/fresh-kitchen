package com.example.freshkitchen.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    CATALOG_NOT_FOUND(HttpStatus.NOT_FOUND, "카탈로그 못핬겠음"),
    // 1. Enum 상수는 필드보다 먼저 선언되어야 합니다.
    USER_ID_REQUIRED(HttpStatus.NOT_FOUND, "아이디가 필요합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    DUPLICATE_USER(HttpStatus.CONFLICT, "이미 존재하는 유저입니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    AI_SETTING(HttpStatus.BAD_REQUEST,"AI 세팅에 값이 없습니다."),
    STORAGE_NOT_FOUND(HttpStatus.BAD_REQUEST,"저장공간을 못 찾겟습니다."),
    //
   INGREDIENT_IMAGE_ALREADY_ATTACHED(HttpStatus.BAD_REQUEST,  "ingredient image is already attached to another ingredient"),
    SYSTEM_DEFAULT_OWNER_MUST_BE_NULL(HttpStatus.BAD_REQUEST,  "user must be null when assetType is SYSTEM_DEFAULT"),
    USER_UPLOAD_OWNER_REQUIRED(HttpStatus.BAD_REQUEST, "user must not be null when assetType is USER_UPLOAD"),
    INGREDIENT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "ingredientId must not be null"),
    INGREDIENT_IMAGE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "ingredientImageId must not be null"),
    INGREDIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ingredient not found"),
    INGREDIENT_IMAGE_NOT_BELONG_TO_INGREDIENT(HttpStatus.BAD_REQUEST, "ingredient image must belong to ingredient"),
    FIRST_IMAGE_MUST_BE_PRIMARY(HttpStatus.BAD_REQUEST, "first ingredient image must be primary"),
    PRIMARY_IMAGE_MUST_BELONG_TO_INGREDIENT(HttpStatus.BAD_REQUEST, "primary image must belong to ingredient"),
    INGREDIENT_PRIMARY_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "ingredient must have one primary image"),
    INGREDIENT_PRIMARY_IMAGE_INVARIANT_BROKEN(HttpStatus.CONFLICT, "ingredient must have exactly one primary image"),
    STORAGE_NOT_OWNED_BY_USER(HttpStatus.BAD_REQUEST, "storage must belong to user"),
    GEMINI_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AI 서비스 일일 할당량이 초과되었습니다. 잠시 후 다시 시도해주세요.");
    private final HttpStatus httpStatus;
    private final String message;
}