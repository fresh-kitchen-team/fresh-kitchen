package com.example.freshkitchen.domain.user.exception;

import com.example.freshkitchen.global.exception.BusinessException;

public class UserException extends BusinessException {

    public UserException(UserErrorCode errorCode) {
        super(errorCode);
    }
}
