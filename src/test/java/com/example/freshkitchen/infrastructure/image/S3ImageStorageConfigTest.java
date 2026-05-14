package com.example.freshkitchen.infrastructure.image;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class S3ImageStorageConfigTest {

    @Test
    void credentialsProvider_usesStaticCredentialsWhenAccessKeyAndSecretKeyAreConfigured() {
        S3ImageStorageProperties properties = properties("access-key", "secret-key");
        AwsCredentialsProvider credentialsProvider = new S3ImageStorageConfig(properties).credentialsProvider();

        AwsCredentials credentials = credentialsProvider.resolveCredentials();

        assertInstanceOf(StaticCredentialsProvider.class, credentialsProvider);
        assertEquals("access-key", credentials.accessKeyId());
        assertEquals("secret-key", credentials.secretAccessKey());
    }

    @Test
    void credentialsProvider_usesDefaultCredentialsProviderWhenAccessKeyIsBlank() {
        S3ImageStorageProperties properties = properties(" ", "secret-key");

        AwsCredentialsProvider credentialsProvider = new S3ImageStorageConfig(properties).credentialsProvider();

        assertInstanceOf(DefaultCredentialsProvider.class, credentialsProvider);
    }

    @Test
    void credentialsProvider_usesDefaultCredentialsProviderWhenSecretKeyIsBlank() {
        S3ImageStorageProperties properties = properties("access-key", null);

        AwsCredentialsProvider credentialsProvider = new S3ImageStorageConfig(properties).credentialsProvider();

        assertInstanceOf(DefaultCredentialsProvider.class, credentialsProvider);
    }

    private static S3ImageStorageProperties properties(String accessKeyId, String secretAccessKey) {
        S3ImageStorageProperties properties = new S3ImageStorageProperties();
        properties.setRegion("ap-northeast-2");
        properties.setBucket("freshkitchen-images");
        properties.setAccessKeyId(accessKeyId);
        properties.setSecretAccessKey(secretAccessKey);
        return properties;
    }
}
