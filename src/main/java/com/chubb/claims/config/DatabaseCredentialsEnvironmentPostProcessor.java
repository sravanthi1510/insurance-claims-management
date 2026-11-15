package com.chubb.claims.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class DatabaseCredentialsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String PROPERTY_PREFIX = "app.database.credentials.";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty(PROPERTY_PREFIX + "enabled", Boolean.class, false)) {
            return;
        }

        String secretName = environment.getRequiredProperty(PROPERTY_PREFIX + "secret-name");
        try (SecretsManagerClient client = createClient(environment)) {
            String secretValue = client.getSecretValue(GetSecretValueRequest.builder()
                            .secretId(secretName)
                            .build())
                    .secretString();
            Map<String, String> credentials = parseCredentials(secretValue);
            environment.getPropertySources().addFirst(new MapPropertySource(
                    "databaseSecretCredentials",
                    Map.of(
                            "spring.datasource.username", credentials.get("username"),
                            "spring.datasource.password", credentials.get("password"))));
        } catch (RuntimeException | IOException exception) {
            throw new IllegalStateException(
                    "Unable to load database credentials from Secrets Manager secret '" + secretName + "'.",
                    exception);
        }
    }

    private SecretsManagerClient createClient(ConfigurableEnvironment environment) {
        return SecretsManagerClient.builder()
                .endpointOverride(URI.create(environment.getRequiredProperty(PROPERTY_PREFIX + "endpoint")))
                .region(Region.of(environment.getRequiredProperty(PROPERTY_PREFIX + "region")))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        environment.getRequiredProperty(PROPERTY_PREFIX + "access-key"),
                        environment.getRequiredProperty(PROPERTY_PREFIX + "secret-key"))))
                .build();
    }

    static Map<String, String> parseCredentials(String secretValue) throws IOException {
        Map<String, String> credentials = OBJECT_MAPPER.readValue(
                secretValue, new TypeReference<>() {
                });
        if (credentials.get("username") == null || credentials.get("username").isBlank()
                || credentials.get("password") == null || credentials.get("password").isBlank()) {
            throw new IllegalArgumentException("The secret must contain non-blank 'username' and 'password' fields.");
        }
        return credentials;
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
