package com.tripmind.configurations.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Khoá ký JWT lấy từ biến môi trường {@code JWT_SECRET}, không có giá trị mặc định trong mã.
 * Thiếu hoặc ngắn hơn 32 byte (256 bit, mức tối thiểu của HS256) thì ứng dụng dừng ngay
 * lúc khởi động thay vì chạy với một khoá ai cũng đoán được.
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank(message = "JWT_SECRET chua duoc dat")
    @Size(min = 32, message = "JWT_SECRET phai dai it nhat 32 ky tu")
    private String secret;

    @Positive
    private long expirationMs;
}
