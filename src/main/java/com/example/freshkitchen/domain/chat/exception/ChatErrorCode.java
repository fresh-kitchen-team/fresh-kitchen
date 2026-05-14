package com.example.freshkitchen.domain.chat.exception;

import com.example.freshkitchen.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ChatErrorCode implements ErrorCode {

    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-404-1", "chat room not found"),
    CHAT_ROOM_NOT_OWNED_BY_USER(HttpStatus.FORBIDDEN, "CHAT-403-1", "chat room does not belong to user"),
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CHAT-503-1", "AI service is currently unavailable"),
    AI_RESPONSE_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT-500-1", "failed to parse AI response"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-404-2", "user not found");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ChatErrorCode(HttpStatus status, String code, String message) {
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
