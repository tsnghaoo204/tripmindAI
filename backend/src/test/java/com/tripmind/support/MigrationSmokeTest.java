package com.tripmind.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationSmokeTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Lược đồ CSDL và dữ liệu khởi tạo đầy đủ (ddl-auto: validate)")
    void migrationsApplyOnEmptyDatabase() {
        Integer usersCount = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        Integer seeded = jdbc.queryForObject("SELECT COUNT(*) FROM destinations", Integer.class);

        assertThat(usersCount).isNotNull();
        assertThat(seeded).isGreaterThanOrEqualTo(15);
    }
}
