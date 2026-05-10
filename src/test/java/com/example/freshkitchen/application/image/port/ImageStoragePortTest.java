package com.example.freshkitchen.application.image.port;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ImageStoragePortTest {

    // --- Command record tests ---

    @Test
    void command_holdsObjectKey() {
        ImageStoragePort.Command command = new ImageStoragePort.Command("images/123/ingredient/uuid.jpg", "image/jpeg");
        assertThat(command.objectKey()).isEqualTo("images/123/ingredient/uuid.jpg");
    }

    @Test
    void command_holdsContentType() {
        ImageStoragePort.Command command = new ImageStoragePort.Command("images/123/ingredient/uuid.jpg", "image/png");
        assertThat(command.contentType()).isEqualTo("image/png");
    }

    @Test
    void command_equality_whenAllFieldsMatch() {
        ImageStoragePort.Command a = new ImageStoragePort.Command("key", "image/jpeg");
        ImageStoragePort.Command b = new ImageStoragePort.Command("key", "image/jpeg");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void command_inequality_whenObjectKeyDiffers() {
        ImageStoragePort.Command a = new ImageStoragePort.Command("key-1", "image/jpeg");
        ImageStoragePort.Command b = new ImageStoragePort.Command("key-2", "image/jpeg");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void command_inequality_whenContentTypeDiffers() {
        ImageStoragePort.Command a = new ImageStoragePort.Command("key", "image/jpeg");
        ImageStoragePort.Command b = new ImageStoragePort.Command("key", "image/png");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void command_hashCode_isConsistentWithEquality() {
        ImageStoragePort.Command a = new ImageStoragePort.Command("key", "image/jpeg");
        ImageStoragePort.Command b = new ImageStoragePort.Command("key", "image/jpeg");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void command_allowsNullObjectKey() {
        ImageStoragePort.Command command = new ImageStoragePort.Command(null, "image/jpeg");
        assertThat(command.objectKey()).isNull();
    }

    @Test
    void command_allowsNullContentType() {
        ImageStoragePort.Command command = new ImageStoragePort.Command("key", null);
        assertThat(command.contentType()).isNull();
    }

    // --- UploadUrl record tests ---

    @Test
    void uploadUrl_holdsObjectKey() {
        OffsetDateTime expiry = OffsetDateTime.now().plusMinutes(15);
        ImageStoragePort.UploadUrl url = new ImageStoragePort.UploadUrl("images/key.jpg", "https://s3/presigned", expiry, "image/jpeg");
        assertThat(url.objectKey()).isEqualTo("images/key.jpg");
    }

    @Test
    void uploadUrl_holdsUploadUrl() {
        OffsetDateTime expiry = OffsetDateTime.now().plusMinutes(15);
        ImageStoragePort.UploadUrl url = new ImageStoragePort.UploadUrl("key", "https://s3/presigned", expiry, "image/jpeg");
        assertThat(url.uploadUrl()).isEqualTo("https://s3/presigned");
    }

    @Test
    void uploadUrl_holdsExpiresAt() {
        OffsetDateTime expiry = OffsetDateTime.parse("2026-06-01T12:00:00+09:00");
        ImageStoragePort.UploadUrl url = new ImageStoragePort.UploadUrl("key", "https://s3/url", expiry, "image/jpeg");
        assertThat(url.expiresAt()).isEqualTo(expiry);
    }

    @Test
    void uploadUrl_holdsContentType() {
        OffsetDateTime expiry = OffsetDateTime.now().plusMinutes(15);
        ImageStoragePort.UploadUrl url = new ImageStoragePort.UploadUrl("key", "https://s3/url", expiry, "image/webp");
        assertThat(url.contentType()).isEqualTo("image/webp");
    }

    @Test
    void uploadUrl_equality_whenAllFieldsMatch() {
        OffsetDateTime expiry = OffsetDateTime.parse("2026-06-01T12:00:00+00:00");
        ImageStoragePort.UploadUrl a = new ImageStoragePort.UploadUrl("key", "https://s3/url", expiry, "image/jpeg");
        ImageStoragePort.UploadUrl b = new ImageStoragePort.UploadUrl("key", "https://s3/url", expiry, "image/jpeg");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void uploadUrl_inequality_whenUrlDiffers() {
        OffsetDateTime expiry = OffsetDateTime.parse("2026-06-01T12:00:00+00:00");
        ImageStoragePort.UploadUrl a = new ImageStoragePort.UploadUrl("key", "https://s3/url-1", expiry, "image/jpeg");
        ImageStoragePort.UploadUrl b = new ImageStoragePort.UploadUrl("key", "https://s3/url-2", expiry, "image/jpeg");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void uploadUrl_allowsNullExpiresAt() {
        ImageStoragePort.UploadUrl url = new ImageStoragePort.UploadUrl("key", "https://url", null, "image/jpeg");
        assertThat(url.expiresAt()).isNull();
    }
}
