package com.example.freshkitchen.domain.ingredient.exception;

import com.example.freshkitchen.global.exception.BusinessException;

public class IngredientException extends BusinessException {

    public IngredientException(IngredientErrorCode errorCode) {
        super(errorCode);
    }
}
