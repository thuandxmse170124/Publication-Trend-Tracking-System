package com.publication_trend_tracking_system.sever_web_app.exception;

import com.publication_trend_tracking_system.sever_web_app.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.Objects;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse> handlingRuntimeException(Exception exception) {

        log.error("Exception: ", exception);

        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(
                ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());

        apiResponse.setMessage(
                ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        return ResponseEntity
                .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode())
                .body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handlingAppException(
            AppException exception) {

        ErrorCode errorCode = exception.getErrorCode();

        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());

        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse> handlingAccessDeniedException(
            AccessDeniedException exception) {

        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handlingValidation(
            MethodArgumentNotValidException exception) {

        String enumKey =
                exception.getFieldError().getDefaultMessage();

        ErrorCode errorCode = ErrorCode.INVALID_KEY;

        Map<String, Object> attributes = null;

        try {

            errorCode = ErrorCode.valueOf(enumKey);

            var constraintViolation =
                    exception.getBindingResult()
                            .getAllErrors()
                            .getFirst()
                            .unwrap(ConstraintViolation.class);

            attributes =
                    constraintViolation
                            .getConstraintDescriptor()
                            .getAttributes();

            log.info(attributes.toString());

        } catch (IllegalArgumentException e) {

            log.error("Validation key not found: {}", enumKey);

        }

        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());

        apiResponse.setMessage(
                Objects.nonNull(attributes)
                        ? mapAttribute(
                        errorCode.getMessage(),
                        attributes)
                        : errorCode.getMessage());

        return ResponseEntity
                .badRequest()
                .body(apiResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiResponse> handlingBadCredentials(
            BadCredentialsException exception) {

        ErrorCode errorCode =
                ErrorCode.UNAUTHENTICATED;

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    private String mapAttribute(
            String message,
            Map<String, Object> attributes) {

        if(attributes.containsKey("min")) {
            message = message.replace(
                    "{min}",
                    String.valueOf(attributes.get("min")));
        }

        if(attributes.containsKey("max")) {
            message = message.replace(
                    "{max}",
                    String.valueOf(attributes.get("max")));
        }

        return message;
    }
}