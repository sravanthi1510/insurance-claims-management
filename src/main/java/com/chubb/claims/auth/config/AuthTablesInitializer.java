package com.chubb.claims.auth.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthTablesInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public AuthTablesInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        createUserTable("customer_db");
        createUserTable("agent_db");
    }

    private void createUserTable(String schema) {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.app_user (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(254) NOT NULL UNIQUE,
                    full_name VARCHAR(120) NOT NULL,
                    password_hash VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema));
    }
}
