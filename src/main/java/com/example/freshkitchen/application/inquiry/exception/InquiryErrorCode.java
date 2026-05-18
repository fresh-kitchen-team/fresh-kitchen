package com.example.freshkitchen.application.inquiry.exception;

import com.example.freshkitchen.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum InquiryErrorCode implements ErrorCode {

    MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "INQUIRY-500-1", "failed to send inquiry email");

    private final HttpStatus status;
    private final String code;
    private final String message;

    InquiryErrorCode(HttpStatus status, String code, String message) {
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
