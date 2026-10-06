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
    @DisplayName("Flyway chạy hết migration và entity khớp lược đồ (ddl-auto: validate)")
    void migrationsApplyOnEmptyDatabase() {
        Integer failed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false", Integer.class);
        Integer seeded = jdbc.queryForObject("SELECT COUNT(*) FROM destinations", Integer.class);

        assertThat(failed).isZero();
        assertThat(seeded).isGreaterThanOrEqualTo(15);
    }
}
