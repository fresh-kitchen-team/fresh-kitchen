package com.example.freshkitchen.domain.chat.exception;

import com.example.freshkitchen.global.exception.BusinessException;

public class ChatException extends BusinessException {
    public ChatException(ChatErrorCode errorCode) {
        super(errorCode);
    }
}
