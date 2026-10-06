package com.tripmind.services;

import com.tripmind.exceptions.AppException;
import com.tripmind.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Giới hạn tần suất theo cửa sổ cố định trên Redis (BR-601).
 *
 * <p>{@code INCR} và {@code EXPIRE} chạy trong <b>một</b> script Lua: tách làm hai lệnh thì
 * tiến trình chết giữa chừng để lại một khoá không có hạn, người dùng bị khoá vĩnh viễn.
 * Redis không dùng được thì cho qua (fail-open) và ghi cảnh báo — mất giới hạn tạm thời còn
 * hơn chặn toàn bộ người dùng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;
    private final Clock clock;

    /** Ném {@code 429 RATE_LIMITED} kèm {@code retryAfterSeconds} khi vượt {@code limit} trong {@code window}. */
    public void check(String scope, Object subject, int limit, Duration window) {
        long windowSeconds = window.toSeconds();
        long now = clock.instant().getEpochSecond();
        long bucket = now / windowSeconds;
        String key = "rl:" + scope + ":" + subject + ":" + bucket;
        Long count;
        try {
            count = redis.execute(INCREMENT, List.of(key), String.valueOf(windowSeconds));
        } catch (Exception e) {
            log.warn("Redis khong dung duoc, bo qua gioi han tan suat {}: {}", scope, e.getMessage());
            return;
        }
        if (count != null && count > limit) {
            long retryAfter = (bucket + 1) * windowSeconds - now;
            throw new AppException(ErrorCode.RATE_LIMITED,
                    "Too many requests, retry after " + retryAfter + " seconds",
                    Map.of("retryAfterSeconds", retryAfter, "limit", limit));
        }
    }
}
