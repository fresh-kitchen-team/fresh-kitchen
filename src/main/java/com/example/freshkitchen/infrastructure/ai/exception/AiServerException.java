package com.example.freshkitchen.infrastructure.ai.exception;

import com.example.freshkitchen.global.exception.BusinessException;

public class AiServerException extends BusinessException {

    public AiServerException(AiServerErrorCode errorCode) {
        super(errorCode);
    }

    public AiServerException(AiServerErrorCode errorCode, Throwable cause) {
        super(errorCode);
        initCause(cause);
    }
}
