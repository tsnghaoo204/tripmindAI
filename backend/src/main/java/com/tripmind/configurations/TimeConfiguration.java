package com.tripmind.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Một nguồn thời gian duy nhất cho mọi phép tính "hôm nay". Test thay bằng
 * {@link Clock#fixed} để kiểm các ca sát nửa đêm mà không phụ thuộc giờ máy.
 */
@Configuration
public class TimeConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
