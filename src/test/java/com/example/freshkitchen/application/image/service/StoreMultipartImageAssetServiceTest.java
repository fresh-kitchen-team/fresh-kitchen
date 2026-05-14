package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.application.image.usecase.StoreMultipartImageAssetUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.domain.image.repository.ImageAssetRepository;
import com.example.freshkitchen.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreMultipartImageAssetServiceTest {

    private final MultipartImageStoragePort multipartImageStoragePort = mock(MultipartImageStoragePort.class);
    private final ImageAssetRepository imageAssetRepository = mock(ImageAssetRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final TransactionOperations transactionOperations = mock(TransactionOperations.class);
    private final StoreMultipartImageAssetService service =
            new StoreMultipartImageAssetService(
                    multipartImageStoragePort,
                    imageAssetRepository,
                    entityManager,
                    transactionOperations
            );
    private final OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-01T14:20:30+09:00");

    @Test
    void store_savesFileAndCreatesImageAsset() {
        runInTransaction();
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "image".getBytes());
        when(multipartImageStoragePort.store(any(MultipartImageStoragePort.Command.class)))
                .thenReturn(new MultipartImageStoragePort.StoredImage(
                        "images/1/receipt/receipt.jpg",
                        StorageProvider.S3,
                        "https://cdn.example.com/images/1/receipt/receipt.jpg"
                ));
        User user = mock(User.class);
        when(entityManager.getReference(User.class, 1L)).thenReturn(user);
        when(imageAssetRepository.save(any(ImageAsset.class))).thenAnswer(invocation -> {
            ImageAsset imageAsset = invocation.getArgument(0);
            ReflectionTestUtils.setField(imageAsset, "id", 11L);
            ReflectionTestUtils.setField(imageAsset, "createdAt", createdAt);
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
                () -> assertEquals(ImageKind.RECEIPT, result.kind()),
                () -> assertEquals(StorageProvider.S3, result.storageProvider()),
                () -> assertEquals("https://cdn.example.com/images/1/receipt/receipt.jpg", result.imageUrl()),
                () -> assertEquals(createdAt, result.createdAt())
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
        verify(multipartImageStoragePort, never()).delete(any(MultipartImageStoragePort.DeleteCommand.class));
    }

    @Test
    void store_deletesStoredImageWhenImageAssetPersistenceFails() {
        runInTransaction();
        RuntimeException failure = new RuntimeException("db unavailable");
        when(multipartImageStoragePort.store(any(MultipartImageStoragePort.Command.class)))
                .thenReturn(new MultipartImageStoragePort.StoredImage(
                        "images/1/receipt/receipt.jpg",
                        StorageProvider.S3,
                        "https://cdn.example.com/images/1/receipt/receipt.jpg"
                ));
        when(entityManager.getReference(User.class, 1L)).thenThrow(failure);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.store(new StoreMultipartImageAssetUseCase.Command(
                        1L,
                        ImageKind.RECEIPT,
                        "receipt.jpg",
                        "image/jpeg",
                        "image".getBytes()
                ))
        );

        assertEquals(failure, thrown);
        ArgumentCaptor<MultipartImageStoragePort.DeleteCommand> captor =
                ArgumentCaptor.forClass(MultipartImageStoragePort.DeleteCommand.class);
        verify(multipartImageStoragePort).delete(captor.capture());
        assertEquals("images/1/receipt/receipt.jpg", captor.getValue().objectKey());
    }

    private void runInTransaction() {
        when(transactionOperations.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }
}
