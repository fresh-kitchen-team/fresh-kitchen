package com.example.freshkitchen.domain.chat.exception;


import com.example.freshkitchen.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ChatErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-404-1", "유저를 찾을 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-404-2", "채팅방을 찾을 수 없습니다."),

    GEMINI_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "CHAT-429-1", "제미나이 호출 과다"),
    CHAT_ROOM_NOT_OWNED_BY_USER(HttpStatus.FORBIDDEN, "CHAT-403-1", "채팅방에 대한 권한이 없습니다."),
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CHAT-503-1", "AI 서비스를 사용할 수 없습니다."),
    AI_RESPONSE_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT-500-1", "AI 응답 파싱에 실패했습니다.");

    GEMINI_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "CHAT-429-1", "제미나이 호출 과다");

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
