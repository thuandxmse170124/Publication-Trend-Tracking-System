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

    @ExceptionHandler(value = {
        org.apache.catalina.connector.ClientAbortException.class,
        org.springframework.web.context.request.async.AsyncRequestNotUsableException.class
    })
    public void handleClientAbortException(Exception exception) {
        log.warn("Client aborted connection or request was no longer usable: {} - {}", 
                 exception.getClass().getSimpleName(), exception.getMessage());
    }

    // Everything below is a caller mistake, not a server fault, and each one used to fall through
    // to the catch-all above and come back as "500 Uncategorized error". A wrong URL reading as an
    // internal error is misleading to whoever is calling the API and hides real 500s in the log.

    /** /api/member/authors reaching /authors/{authorId}: "authors" is not a Long. */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException exception) {

        log.warn("Bad parameter '{}' = '{}'", exception.getName(), exception.getValue());
        return build(ErrorCode.INVALID_PARAMETER);
    }

    /** A required query parameter was left out entirely. */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            org.springframework.web.bind.MissingServletRequestParameterException exception) {

        log.warn("Missing required parameter '{}'", exception.getParameterName());
        return build(ErrorCode.INVALID_PARAMETER);
    }

    /** Body present but not parseable — malformed JSON, wrong field type. */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleUnreadableBody(
            org.springframework.http.converter.HttpMessageNotReadableException exception) {

        log.warn("Unreadable request body: {}", exception.getMessage());
        return build(ErrorCode.INVALID_PARAMETER);
    }

    /** No controller matched the path. Needs spring.mvc.throw-exception-if-no-handler-found. */
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNoHandler(
            org.springframework.web.servlet.NoHandlerFoundException exception) {

        log.warn("No handler for {} {}", exception.getHttpMethod(), exception.getRequestURL());
        return build(ErrorCode.ENDPOINT_NOT_FOUND);
    }

    /** Path exists, verb does not — POST to a GET-only endpoint. */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException exception) {

        log.warn("Method {} not supported here", exception.getMethod());
        return build(ErrorCode.METHOD_NOT_ALLOWED);
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode errorCode) {
        ApiResponse<Void> apiResponse = new ApiResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<Void>> handlingRuntimeException(Exception exception) {

        log.error("Exception: ", exception);

        ApiResponse<Void> apiResponse = new ApiResponse<>();

        apiResponse.setCode(
                ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());

        apiResponse.setMessage(
                ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        return ResponseEntity
                .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode())
                .body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<Void>> handlingAppException(
            AppException exception) {

        ErrorCode errorCode = exception.getErrorCode();

        ApiResponse<Void> apiResponse = new ApiResponse<>();

        apiResponse.setCode(errorCode.getCode());

        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handlingAccessDeniedException(
            AccessDeniedException exception) {

        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handlingValidation(
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

        ApiResponse<Void> apiResponse = new ApiResponse<>();

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
    ResponseEntity<ApiResponse<Void>> handlingBadCredentials(
            BadCredentialsException exception) {

        ErrorCode errorCode =
                ErrorCode.UNAUTHENTICATED;

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.<Void>builder()
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