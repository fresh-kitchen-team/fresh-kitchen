package com.example.freshkitchen.infrastructure.image;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class S3MultipartImageStorageAdapterTest {

    private final S3Client s3Client = mock(S3Client.class);
    private final S3ImageStorageProperties properties = properties();
    private final S3MultipartImageStorageAdapter adapter =
            new S3MultipartImageStorageAdapter(s3Client, properties, imageStorageUrlFactory(properties));

    @Test
    void store_uploadsObjectAndReturnsS3ImageMetadata() {
        MultipartImageStoragePort.StoredImage storedImage = adapter.store(new MultipartImageStoragePort.Command(
                1L,
                ImageKind.INGREDIENT,
                "tomato.jpg",
                " IMAGE/WEBP ",
                "image".getBytes()
        ));

        assertAll(
                () -> assertTrue(storedImage.objectKey().startsWith("images/1/ingredient/")),
                () -> assertTrue(storedImage.objectKey().endsWith(".webp")),
                () -> assertEquals(StorageProvider.S3, storedImage.storageProvider()),
                () -> assertEquals("https://cdn.example.com/" + storedImage.objectKey(), storedImage.imageUrl())
        );

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertAll(
                () -> assertEquals("freshkitchen-images", captor.getValue().bucket()),
                () -> assertEquals(storedImage.objectKey(), captor.getValue().key()),
                () -> assertEquals("image/webp", captor.getValue().contentType())
        );
    }

    @Test
    void store_rejectsNullContentType() {
        assertThrows(BusinessValidationException.class, () -> adapter.store(command(null)));
    }

    @Test
    void store_rejectsBlankContentType() {
        assertThrows(BusinessValidationException.class, () -> adapter.store(command(" ")));
    }

    @Test
    void store_rejectsUnsupportedContentType() {
        assertThrows(BusinessValidationException.class, () -> adapter.store(command("image/gif")));
    }

    private static MultipartImageStoragePort.Command command(String contentType) {
        return new MultipartImageStoragePort.Command(
                1L,
                ImageKind.INGREDIENT,
                "tomato.jpg",
                contentType,
                "image".getBytes()
        );
    }

    private static S3ImageStorageProperties properties() {
        S3ImageStorageProperties properties = new S3ImageStorageProperties();
        properties.setRegion("ap-northeast-2");
        properties.setBucket("freshkitchen-images");
        properties.setAccessKeyId("access-key");
        properties.setSecretAccessKey("secret-key");
        properties.setPublicBaseUrl("https://cdn.example.com");
        return properties;
    }

    private static ImageStorageUrlFactory imageStorageUrlFactory(S3ImageStorageProperties s3Properties) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("s3ImageStorageProperties", s3Properties);
        return new ImageStorageUrlFactory(new LocalImageStorageProperties(), beanFactory.getBeanProvider(
                S3ImageStorageProperties.class));
    }
}
