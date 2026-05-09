package com.example.freshkitchen.domain.chat.exception;


import com.example.freshkitchen.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ChatErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"USER001","유저를 찾을 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND,"CHAT001","채팅방을 찾을 수 없습니다."),
    GEMINI_QUOTA_EXCEEDED(HttpStatus.NOT_FOUND,"Gemini002","제미나이 호출 과다");


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
