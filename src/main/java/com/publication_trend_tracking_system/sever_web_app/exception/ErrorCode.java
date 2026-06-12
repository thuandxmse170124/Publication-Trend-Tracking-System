package com.publication_trend_tracking_system.sever_web_app.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Common
    UNCATEGORIZED_EXCEPTION(
            9999,
            "Uncategorized error",
            HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_KEY(
            9998,
            "Invalid message key",
            HttpStatus.BAD_REQUEST),

    // Authentication
    UNAUTHENTICATED(
            1001,
            "Email or password incorrect",
            HttpStatus.UNAUTHORIZED),

    UNAUTHORIZED(
            1002,
            "You do not have permission",
            HttpStatus.FORBIDDEN),

    // User
    USER_NOT_FOUND(
            1101,
            "User not found",
            HttpStatus.NOT_FOUND),

    EMAIL_EXISTED(
            1102,
            "Email already exists",
            HttpStatus.BAD_REQUEST),

    ROLE_NOT_FOUND(
            1103,
            "Role not found",
            HttpStatus.NOT_FOUND),

    OLD_PASSWORD_INCORRECT(
            1104,
            "Old password incorrect",
            HttpStatus.BAD_REQUEST),

    // Validation
    FULLNAME_REQUIRED(
            1201,
            "Full name is required",
            HttpStatus.BAD_REQUEST),

    INVALID_FULLNAME(
            1201,
            "Full name must be between {min} and {max} characters",
            HttpStatus.BAD_REQUEST),

    EMAIL_REQUIRED(
            1207,
            "Email is required",
            HttpStatus.BAD_REQUEST),

    INVALID_EMAIL(
            1203,
            "Invalid email format",
            HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(
            1204,
            "Password is required",
            HttpStatus.BAD_REQUEST),

    INVALID_PASSWORD(
            1205,
            "Password must be at least {min} characters",
            HttpStatus.BAD_REQUEST),

    PASSWORD_INVALID_FORMAT(
            1206,
            "Password must contain at least one uppercase letter and one special character",
            HttpStatus.BAD_REQUEST),
    // Research Paper
    PAPER_NOT_FOUND(
            1301,
            "Research paper not found",
            HttpStatus.NOT_FOUND),

    // Bookmark
    BOOKMARK_NOT_FOUND(
            1401,
            "Bookmark not found",
            HttpStatus.NOT_FOUND),

    // Report Ticket
    REPORT_NOT_FOUND(
            1501,
            "Report ticket not found",
            HttpStatus.NOT_FOUND),

    // Topic
    TOPIC_NOT_FOUND(
            1601,
            "Research topic not found",
            HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(
            int code,
            String message,
            HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}