package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.port.ImageStoragePort;
import com.example.freshkitchen.application.image.usecase.CreateImageUploadUrlUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateImageUploadUrlServiceTest {

    private final CreateImageUploadUrlUseCase createImageUploadUrlUseCase =
            new CreateImageUploadUrlService(new FakeImageStoragePort());

    @Test
    void create_returnsPresignedUploadUrl() {
        CreateImageUploadUrlUseCase.Result result = createImageUploadUrlUseCase.create(
                new CreateImageUploadUrlUseCase.Command(
                        1L,
                        ImageKind.INGREDIENT,
                        "tomato.JPG",
                        "image/jpeg"
                )
        );

        assertTrue(result.objectKey().startsWith("images/1/ingredient/"));
        assertTrue(result.objectKey().endsWith(".jpg"));
        assertEquals("https://example.test/upload/" + result.objectKey(), result.uploadUrl());
        assertEquals("image/jpeg", result.contentType());
        assertNotNull(result.expiresAt());
    }

    @Test
    void create_rejectsBlankContentType() {
        assertThrows(
                BusinessValidationException.class,
                () -> createImageUploadUrlUseCase.create(new CreateImageUploadUrlUseCase.Command(
                        1L,
                        ImageKind.INGREDIENT,
                        "tomato.jpg",
                        " "
                ))
        );
    }

    @Test
    void create_rejectsBlankOriginalFileName() {
        assertThrows(
                BusinessValidationException.class,
                () -> createImageUploadUrlUseCase.create(new CreateImageUploadUrlUseCase.Command(
                        1L,
                        ImageKind.INGREDIENT,
                        " ",
                        "image/jpeg"
                ))
        );
    }

    private static class FakeImageStoragePort implements ImageStoragePort {

        @Override
        public UploadUrl createUploadUrl(Command command) {
            return new UploadUrl(
                    command.objectKey(),
                    "https://example.test/upload/" + command.objectKey(),
                    OffsetDateTime.parse("2026-05-06T12:10:00+09:00"),
                    command.contentType()
            );
        }
    }
}
