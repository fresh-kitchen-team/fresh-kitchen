package com.example.freshkitchen.domain.ingredient.exception;

import com.example.freshkitchen.global.exception.BaseException;

public class IngredientException extends BaseException {

    public IngredientException(IngredientErrorCode errorCode) {
        super(errorCode);
    }
}
