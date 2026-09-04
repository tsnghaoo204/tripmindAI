package com.tripmind.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Requested resource was not found"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "User is unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access is forbidden"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed for request parameters"),
    PROPOSAL_NOT_PENDING(HttpStatus.CONFLICT, "AI Proposal is not in PENDING state"),
    PROPOSAL_EXPIRED(HttpStatus.CONFLICT, "AI Proposal has expired"),
    CONFLICT(HttpStatus.CONFLICT, "Resource conflict occurred"),
    EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "External API service unavailable"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
