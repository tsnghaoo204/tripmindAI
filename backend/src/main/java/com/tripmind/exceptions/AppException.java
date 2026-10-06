package com.tripmind.exceptions;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    /** Dữ liệu kèm theo lỗi, trả về ở trường {@code details} (ví dụ danh sách ngày còn hoạt động). */
    private final transient Object details;

    public AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null);
    }

    public AppException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public AppException(ErrorCode errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
}
