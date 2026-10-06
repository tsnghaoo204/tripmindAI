package com.tripmind.domains.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    /** Mã lỗi ({@link com.tripmind.exceptions.ErrorCode}); chỉ có khi {@code success = false}. */
    private String code;
    private String message;
    private T data;
    /** Dữ liệu kèm theo lỗi; chỉ có khi {@code success = false}. */
    private Object details;
    @Builder.Default
    private Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> fail(String code, String message, Object details) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .details(details)
                .build();
    }
}
