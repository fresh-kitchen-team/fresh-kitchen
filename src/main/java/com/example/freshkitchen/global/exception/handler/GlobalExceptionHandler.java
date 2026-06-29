package com.example.freshkitchen.global.exception.handler;

import com.example.freshkitchen.global.exception.BusinessException;
import com.example.freshkitchen.global.exception.CommonErrorCode;
import com.example.freshkitchen.global.exception.ErrorCode;
import com.example.freshkitchen.global.exception.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        logHandledException(errorCode.status(), errorCode.code(), request, exception);
        return buildResponse(errorCode, request);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            MissingRequestHeaderException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidWebRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return handleInvalidInput(exception, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return handleInvalidInput(exception, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return handleInvalidInput(exception, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return handleInvalidInput(exception, request);
    }

    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MultipartException.class
    })
    public ResponseEntity<ErrorResponse> handleMultipartException(
            Exception exception,
            HttpServletRequest request
    ) {
        return handleInvalidInput(exception, request);
    }

    private ResponseEntity<ErrorResponse> handleInvalidInput(
            Exception exception,
            HttpServletRequest request
    ) {
        logHandledException(
                CommonErrorCode.INVALID_INPUT.status(),
                CommonErrorCode.INVALID_INPUT.code(),
                request,
                exception
        );
        return buildResponse(CommonErrorCode.INVALID_INPUT, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException exception,
            HttpServletRequest request
    ) {
        logHandledException(
                CommonErrorCode.INVALID_STATE.status(),
                CommonErrorCode.INVALID_STATE.code(),
                request,
                exception
        );
        return buildResponse(CommonErrorCode.INVALID_STATE, request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception: code={}, status={}, path={}, type={}, message={}",
                CommonErrorCode.INTERNAL_SERVER_ERROR.code(),
                CommonErrorCode.INTERNAL_SERVER_ERROR.status().value(),
                request.getRequestURI(),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                exception
        );
        return buildResponse(
                CommonErrorCode.INTERNAL_SERVER_ERROR,
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            ErrorCode errorCode,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(errorCode.status())
                .body(ErrorResponse.of(errorCode, resolveResponseMessage(errorCode), request.getRequestURI()));
    }

    private String resolveResponseMessage(ErrorCode errorCode) {
        return errorCode.message();
    }

    private void logHandledException(
            HttpStatus status,
            String code,
            HttpServletRequest request,
            Exception exception
    ) {
        if (status.is5xxServerError()) {
            log.error(
                    "Handled exception: code={}, status={}, path={}, type={}, message={}",
                    code,
                    status.value(),
                    request.getRequestURI(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception
            );
            return;
        }

        log.debug(
                "Handled exception: code={}, status={}, path={}, type={}, message={}",
                code,
                status.value(),
                request.getRequestURI(),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                exception
        );
    }
}
