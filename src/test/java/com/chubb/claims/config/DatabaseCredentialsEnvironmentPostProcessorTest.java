package com.chubb.claims.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseCredentialsEnvironmentPostProcessorTest {

    @Test
    void parsesUsernameAndPasswordFromSecretJson() throws Exception {
        assertEquals(
                Map.of("username", "claims_app", "password", "secret-value"),
                DatabaseCredentialsEnvironmentPostProcessor.parseCredentials(
                        "{\"username\":\"claims_app\",\"password\":\"secret-value\"}"));
    }

    @Test
    void rejectsSecretsWithoutBothCredentials() {
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseCredentialsEnvironmentPostProcessor.parseCredentials(
                        "{\"username\":\"claims_app\"}"));
    }
}
