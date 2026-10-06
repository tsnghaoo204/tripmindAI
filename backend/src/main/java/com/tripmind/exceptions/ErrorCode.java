package com.tripmind.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Mã lỗi trả về trong trường {@code code} của phản hồi lỗi. Giao diện rẽ nhánh theo mã,
 * không theo câu chữ của {@code message}.
 */
@Getter
public enum ErrorCode {
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Requested resource was not found"),
    PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "AI proposal was not found"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "User is unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access is forbidden"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed for request parameters"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "End date must not be before start date"),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "End time must not be before start time"),
    TRIP_TOO_LONG(HttpStatus.UNPROCESSABLE_ENTITY, "Trip is longer than the allowed number of days"),
    DAYS_HAVE_ACTIVITIES(HttpStatus.UNPROCESSABLE_ENTITY, "Days to be removed still contain activities"),
    REORDER_SET_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "Reorder list does not match the activities of the day"),
    CURRENCY_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "Currency differs from the trip currency"),
    TRIP_NOT_ENDED(HttpStatus.CONFLICT, "Trip has not ended yet"),
    TRIP_NOT_EMPTY(HttpStatus.CONFLICT, "Trip already has activities"),
    PROPOSAL_NOT_PENDING(HttpStatus.CONFLICT, "AI Proposal is not in PENDING state"),
    PROPOSAL_EXPIRED(HttpStatus.CONFLICT, "AI Proposal has expired"),
    PROPOSAL_STALE(HttpStatus.CONFLICT, "Itinerary changed since the proposal was created"),
    PROPOSAL_NOT_APPLIED(HttpStatus.CONFLICT, "AI Proposal is not in APPLIED state"),
    UNDO_WINDOW_CLOSED(HttpStatus.CONFLICT, "Undo window has closed"),
    UNDO_BLOCKED_BY_LATER(HttpStatus.CONFLICT, "A later applied proposal touches the same activities"),
    UNDO_NOT_AVAILABLE(HttpStatus.CONFLICT, "Proposal has no undo log"),
    CONFLICT(HttpStatus.CONFLICT, "Resource conflict occurred"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
    AI_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "AI assistant is not configured"),
    AI_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "AI model provider failed"),
    EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "External API service unavailable"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
