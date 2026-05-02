package com.example.freshkitchen.global.security.exception;

import com.example.freshkitchen.global.exception.BusinessException;

public class JwtException extends BusinessException {

    public JwtException(JwtErrorCode errorCode) {
        super(errorCode);
    }
}
