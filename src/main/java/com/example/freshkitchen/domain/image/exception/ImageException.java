package com.example.freshkitchen.domain.image.exception;

import com.example.freshkitchen.global.exception.BusinessException;

public class ImageException extends BusinessException {

    public ImageException(ImageErrorCode errorCode) {
        super(errorCode);
    }
}
