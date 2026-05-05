package com.example.freshkitchen.global.security.exception;

import com.example.freshkitchen.global.exception.BusinessException;

public class JwtTokenException extends BusinessException {

    public JwtTokenException(JwtErrorCode errorCode) {
        this(errorCode, null);
    }

    public JwtTokenException(JwtErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
