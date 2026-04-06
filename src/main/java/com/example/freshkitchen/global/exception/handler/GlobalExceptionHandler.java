package com.example.freshkitchen.global.exception.handler;

import com.example.freshkitchen.global.exception.BaseException;
import com.example.freshkitchen.global.exception.CommonErrorCode;
import com.example.freshkitchen.global.exception.ErrorCode;
import com.example.freshkitchen.global.exception.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            BaseException exception,
            HttpServletRequest request
    ) {
        return buildResponse(exception.getErrorCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return buildResponse(CommonErrorCode.INVALID_INPUT, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException exception,
            HttpServletRequest request
    ) {
        return buildResponse(CommonErrorCode.INVALID_STATE, exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                CommonErrorCode.INTERNAL_SERVER_ERROR,
                CommonErrorCode.INTERNAL_SERVER_ERROR.message(),
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            ErrorCode errorCode,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(errorCode.status())
                .body(ErrorResponse.of(errorCode, message, request.getRequestURI()));
    }
}
