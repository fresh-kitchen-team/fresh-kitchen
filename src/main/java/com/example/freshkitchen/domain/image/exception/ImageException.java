package com.example.freshkitchen.domain.image.exception;

import com.example.freshkitchen.global.exception.BaseException;

public class ImageException extends BaseException {

    public ImageException(ImageErrorCode errorCode) {
        super(errorCode);
    }
}
