package com.example.freshkitchen.global.exception;

public class BusinessValidationException extends BaseException {

    public BusinessValidationException(String detailMessage) {
        super(CommonErrorCode.INVALID_INPUT, detailMessage);
    }
}
