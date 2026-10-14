package com.tripmind.domains.requests;

/** Giới hạn dùng chung cho các trường văn bản tự do trong request. */
public final class ValidationLimits {

    private ValidationLimits() {}

    /** Chỉ chấp nhận URL https, không khoảng trắng hay ký tự chèn HTML. */
    public static final String HTTPS_URL = "^https://[^\\s<>\"']+$";
    public static final String HTTPS_URL_MESSAGE = "URL phai bat dau bang https:// va khong chua khoang trang";
    public static final int URL_MAX = 2048;
    public static final int NOTES_MAX = 2000;
    public static final int EXTERNAL_ID_MAX = 255;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 72;
}
