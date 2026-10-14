package com.tripmind.configurations;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    // Dùng RedisConnectionDetails để Testcontainers @ServiceConnection cũng áp dụng cho Redisson
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisConnectionDetails details) {
        RedisConnectionDetails.Standalone standalone = details.getStandalone();
        Config config = new Config();
        String address = "redis://" + standalone.getHost() + ":" + standalone.getPort();
        var serverConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(standalone.getDatabase())
                .setConnectionMinimumIdleSize(5)
                .setConnectionPoolSize(20)
                .setConnectTimeout(5000)
                .setTimeout(3000);

        String password = details.getPassword();
        if (password != null && !password.isBlank()) {
            serverConfig.setPassword(password);
        }
        return Redisson.create(config);
    }
}
