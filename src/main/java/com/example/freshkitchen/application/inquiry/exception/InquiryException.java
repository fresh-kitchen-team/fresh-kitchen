package com.example.freshkitchen.application.inquiry.exception;

import com.example.freshkitchen.global.exception.BusinessException;

public class InquiryException extends BusinessException {

    public InquiryException(InquiryErrorCode errorCode) {
        super(errorCode);
    }

    public InquiryException(InquiryErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.message(), cause);
    }
}
