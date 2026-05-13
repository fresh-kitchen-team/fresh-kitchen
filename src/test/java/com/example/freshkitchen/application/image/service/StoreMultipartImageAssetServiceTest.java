package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.repository.ImageAssetRepository;
import com.example.freshkitchen.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreMultipartImageAssetServiceTest {

    private final MultipartImageStoragePort multipartImageStoragePort = mock(MultipartImageStoragePort.class);
    private final ImageAssetRepository imageAssetRepository = mock(ImageAssetRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final StoreMultipartImageAssetService service =
            new StoreMultipartImageAssetService(multipartImageStoragePort, imageAssetRepository, entityManager);

    @Test
    void store_savesFileAndCreatesImageAsset() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "image".getBytes());
        when(multipartImageStoragePort.store(any(MultipartImageStoragePort.Command.class)))
                .thenReturn(new MultipartImageStoragePort.StoredImage("/uploads/images/1/receipt/receipt.jpg"));
        User user = mock(User.class);
        when(entityManager.getReference(User.class, 1L)).thenReturn(user);
        when(imageAssetRepository.save(any(ImageAsset.class))).thenAnswer(invocation -> {
            ImageAsset imageAsset = invocation.getArgument(0);
            ReflectionTestUtils.setField(imageAsset, "id", 11L);
            return imageAsset;
        });

        StoreMultipartImageAssetUseCase.Result result = service.store(
                new StoreMultipartImageAssetUseCase.Command(
                        1L,
                        ImageKind.RECEIPT,
                        file.getOriginalFilename(),
                        file.getContentType(),
                        "image".getBytes()
                )
        );

        assertAll(
                () -> assertEquals(11L, result.imageAssetId()),
                () -> assertEquals("/uploads/images/1/receipt/receipt.jpg", result.imageUrl())
        );
        ArgumentCaptor<MultipartImageStoragePort.Command> captor =
                ArgumentCaptor.forClass(MultipartImageStoragePort.Command.class);
        verify(multipartImageStoragePort).store(captor.capture());
        assertAll(
                () -> assertEquals(1L, captor.getValue().userId()),
                () -> assertEquals(ImageKind.RECEIPT, captor.getValue().kind()),
                () -> assertEquals("receipt.jpg", captor.getValue().originalFilename()),
                () -> assertEquals("image/jpeg", captor.getValue().contentType()),
                () -> assertArrayEquals("image".getBytes(), captor.getValue().content())
        );
    }
}
