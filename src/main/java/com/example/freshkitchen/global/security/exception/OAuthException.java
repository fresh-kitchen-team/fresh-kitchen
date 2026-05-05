package com.example.freshkitchen.global.security.exception;

import com.example.freshkitchen.global.exception.BusinessException;

public class OAuthException extends BusinessException {

    public OAuthException(OAuthErrorCode errorCode) {
        super(errorCode);
    }

    public OAuthException(OAuthErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
