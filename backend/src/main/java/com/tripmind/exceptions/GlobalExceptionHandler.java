package com.tripmind.exceptions;

import com.tripmind.domains.responses.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.fail(code.name(), ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                errors.put(error.getObjectName(), error.getDefaultMessage()));
        return badRequest("Validation failed", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        return badRequest("Validation failed", errors);
    }

    /** JSON hỏng, sai kiểu, hoặc giá trị enum không hợp lệ (ví dụ {@code "status": "FOO"}). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        String cause = ex.getMostSpecificCause().getMessage();
        return badRequest("Malformed request body", cause);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse<Object>> handleBadParameter(Exception ex) {
        return badRequest(ex.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ErrorCode.FORBIDDEN.name(), ex.getMessage(), null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ErrorCode.RESOURCE_NOT_FOUND.name(), ex.getMessage(), null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(ErrorCode.VALIDATION_ERROR.name(), ex.getMessage(), null));
    }

    /** Ràng buộc CSDL chặn lại (trùng khoá, vi phạm CHECK) mà tầng dịch vụ chưa bắt trước. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Rang buoc CSDL tu choi ghi: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(ErrorCode.CONFLICT.name(), "Data constraint violated", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception ex) {
        log.error("Loi khong luong truoc", ex);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        "An unexpected error occurred", null));
    }

    private ResponseEntity<ApiResponse<Object>> badRequest(String message, Object details) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.VALIDATION_ERROR.name(), message, details));
    }
}
