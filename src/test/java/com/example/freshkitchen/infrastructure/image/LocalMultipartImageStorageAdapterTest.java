package com.example.freshkitchen.infrastructure.image;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalMultipartImageStorageAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void store_normalizesContentTypeAndStoresLocalImage() throws IOException {
        LocalImageStorageProperties properties = properties();
        LocalMultipartImageStorageAdapter adapter = new LocalMultipartImageStorageAdapter(
                properties,
                imageStorageUrlFactory(properties)
        );

        MultipartImageStoragePort.StoredImage storedImage = adapter.store(command(" IMAGE/PNG "));

        Path storedFile = tempDir.resolve(storedImage.objectKey());
        assertAll(
                () -> assertTrue(storedImage.objectKey().startsWith("images/1/ingredient/")),
                () -> assertTrue(storedImage.objectKey().endsWith(".png")),
                () -> assertEquals(StorageProvider.LOCAL, storedImage.storageProvider()),
                () -> assertEquals("/uploads/" + storedImage.objectKey(), storedImage.imageUrl()),
                () -> assertTrue(Files.exists(storedFile)),
                () -> assertArrayEquals("image".getBytes(), Files.readAllBytes(storedFile))
        );
    }

    @Test
    void store_rejectsNullContentType() {
        LocalImageStorageProperties properties = properties();
        LocalMultipartImageStorageAdapter adapter = new LocalMultipartImageStorageAdapter(
                properties,
                imageStorageUrlFactory(properties)
        );

        assertThrows(BusinessValidationException.class, () -> adapter.store(command(null)));
    }

    @Test
    void store_rejectsBlankContentType() {
        LocalImageStorageProperties properties = properties();
        LocalMultipartImageStorageAdapter adapter = new LocalMultipartImageStorageAdapter(
                properties,
                imageStorageUrlFactory(properties)
        );

        assertThrows(BusinessValidationException.class, () -> adapter.store(command(" ")));
    }

    @Test
    void store_rejectsUnsupportedContentType() {
        LocalImageStorageProperties properties = properties();
        LocalMultipartImageStorageAdapter adapter = new LocalMultipartImageStorageAdapter(
                properties,
                imageStorageUrlFactory(properties)
        );

        assertThrows(BusinessValidationException.class, () -> adapter.store(command("image/gif")));
    }

    private MultipartImageStoragePort.Command command(String contentType) {
        return new MultipartImageStoragePort.Command(
                1L,
                ImageKind.INGREDIENT,
                "tomato.png",
                contentType,
                "image".getBytes()
        );
    }

    private LocalImageStorageProperties properties() {
        LocalImageStorageProperties properties = new LocalImageStorageProperties();
        properties.setRootDir(tempDir.toString());
        properties.setPublicBaseUrl("/uploads");
        return properties;
    }

    private static ImageStorageUrlFactory imageStorageUrlFactory(LocalImageStorageProperties properties) {
        return new ImageStorageUrlFactory(
                properties,
                new DefaultListableBeanFactory().getBeanProvider(S3ImageStorageProperties.class)
        );
    }
}
