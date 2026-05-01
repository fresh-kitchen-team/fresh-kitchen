package com.example.freshkitchen.global.security.exception;

import com.example.freshkitchen.global.exception.BusinessException;

public class CustomJwtException extends BusinessException {

    public CustomJwtException(JwtErrorCode errorCode) {
        super(errorCode);
    }
}
