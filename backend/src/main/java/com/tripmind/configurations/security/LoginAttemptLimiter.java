package com.tripmind.configurations.security;

import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chống dò mật khẩu: đếm số lần đăng nhập SAI theo email và theo IP trong một cửa sổ. Vượt
 * ngưỡng thì trả 429 kể cả khi mật khẩu đúng, tới khi cửa sổ hết hạn. Redis hỏng thì cho qua.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptLimiter {

    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;

    @Value("${tripmind.rate-limit.login-failures-per-email:5}")
    private int perEmail;

    @Value("${tripmind.rate-limit.login-failures-per-ip:20}")
    private int perIp;

    @Value("${tripmind.rate-limit.login-window:PT5M}")
    private Duration window;

    /** Ném 429 khi email hoặc IP đã sai quá số lần cho phép. */
    public void ensureAllowed(String email, String ip) {
        check(emailKey(email), perEmail);
        check(ipKey(ip), perIp);
    }

    public void recordFailure(String email, String ip) {
        increment(emailKey(email));
        increment(ipKey(ip));
    }

    /** Đăng nhập đúng thì xoá bộ đếm theo email (bộ đếm IP giữ nguyên). */
    public void reset(String email) {
        try {
            redis.delete(emailKey(email));
        } catch (Exception e) {
            log.warn("Redis khong dung duoc, khong xoa duoc bo dem dang nhap: {}", e.getMessage());
        }
    }

    private void check(String key, int limit) {
        String value;
        Long ttl;
        try {
            value = redis.opsForValue().get(key);
            ttl = value == null ? null : redis.getExpire(key);
        } catch (Exception e) {
            log.warn("Redis khong dung duoc, bo qua gioi han dang nhap: {}", e.getMessage());
            return;
        }
        if (value != null && Long.parseLong(value) >= limit) {
            long retryAfter = ttl == null || ttl < 0 ? window.toSeconds() : ttl;
            throw new AppException(ErrorCode.RATE_LIMITED,
                    "Too many failed login attempts, retry after " + retryAfter + " seconds",
                    Map.of("retryAfterSeconds", retryAfter));
        }
    }

    private void increment(String key) {
        try {
            redis.execute(INCREMENT, List.of(key), String.valueOf(window.toSeconds()));
        } catch (Exception e) {
            log.warn("Redis khong dung duoc, khong dem duoc lan dang nhap sai: {}", e.getMessage());
        }
    }

    private static String emailKey(String email) {
        return "login-fail:email:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private static String ipKey(String ip) {
        return "login-fail:ip:" + ip;
    }
}
