package com.example.freshkitchen.global.exception;

public class BusinessValidationException extends BusinessException {

    public BusinessValidationException(String detailMessage) {
        super(CommonErrorCode.INVALID_INPUT, detailMessage);
    }

    public BusinessValidationException(String detailMessage, Throwable cause) {
        super(CommonErrorCode.INVALID_INPUT, detailMessage, cause);
    }
}
