package com.example.freshkitchen.application.inquiry.service;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.application.inquiry.usecase.SendInquiryUseCase;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.inquiry.entity.Inquiry;
import com.example.freshkitchen.domain.inquiry.enums.InquiryCategory;
import com.example.freshkitchen.domain.inquiry.enums.InquiryType;
import com.example.freshkitchen.domain.inquiry.repository.InquiryRepository;
import com.example.freshkitchen.infrastructure.inquiry.InquiryProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class SendInquiryServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final InquiryProperties inquiryProperties = mock(InquiryProperties.class);
    private final InquiryRepository inquiryRepository = mock(InquiryRepository.class);
    private final MultipartImageStoragePort imageStorage = mock(MultipartImageStoragePort.class);

    private final SendInquiryService service = new SendInquiryService(
            mailSender, inquiryProperties, inquiryRepository, imageStorage
    );

    @BeforeEach
    void initSync() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearSync() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void send_withImage_uploadsAndStoresUrl() throws Exception {
        // given
        MultipartFile image = mock(MultipartFile.class);
        given(image.isEmpty()).willReturn(false);
        given(image.getOriginalFilename()).willReturn("photo.jpg");
        given(image.getContentType()).willReturn("image/jpeg");
        given(image.getBytes()).willReturn(new byte[]{1, 2, 3});

        given(imageStorage.store(any())).willReturn(
                new MultipartImageStoragePort.StoredImage(
                        "images/1/inquiry/uuid.jpg",
                        com.example.freshkitchen.domain.image.enums.StorageProvider.S3,
                        "https://cdn.example.com/images/1/inquiry/uuid.jpg"
                )
        );

        Inquiry saved = Inquiry.create(new Inquiry.CreateCommand(
                1L, InquiryType.INQUIRY, InquiryCategory.RECIPE, "test content", "https://cdn.example.com/images/1/inquiry/uuid.jpg"
        ));
        ReflectionTestUtils.setField(saved, "id", 1L);
        given(inquiryRepository.save(any(Inquiry.class))).willReturn(saved);
        given(inquiryProperties.getAdminEmail()).willReturn("admin@test.com");

        // when
        Long inquiryId = service.send(new SendInquiryUseCase.Command(
                1L, InquiryType.INQUIRY, InquiryCategory.RECIPE, "test content", image
        ));

        // then
        assertThat(inquiryId).isEqualTo(1L);
        verify(imageStorage).store(any());
        verify(inquiryRepository).save(argThat(inquiry ->
                inquiry.getImageUrl() != null && inquiry.getImageUrl().contains("uuid.jpg")
        ));
    }

    @Test
    void send_withImageUploadFailure_stillSavesInquiry() throws Exception {
        // given
        MultipartFile image = mock(MultipartFile.class);
        given(image.isEmpty()).willReturn(false);
        given(image.getOriginalFilename()).willReturn("photo.jpg");
        given(image.getContentType()).willReturn("image/jpeg");
        given(image.getBytes()).willReturn(new byte[]{1, 2, 3});

        given(imageStorage.store(any())).willThrow(new RuntimeException("S3 down"));

        Inquiry saved = Inquiry.create(new Inquiry.CreateCommand(
                1L, InquiryType.INQUIRY, InquiryCategory.RECIPE, "test content", null
        ));
        ReflectionTestUtils.setField(saved, "id", 2L);
        given(inquiryRepository.save(any(Inquiry.class))).willReturn(saved);
        given(inquiryProperties.getAdminEmail()).willReturn("admin@test.com");

        // when
        Long inquiryId = service.send(new SendInquiryUseCase.Command(
                1L, InquiryType.INQUIRY, InquiryCategory.RECIPE, "test content", image
        ));

        // then
        assertThat(inquiryId).isEqualTo(2L);
        verify(inquiryRepository).save(argThat(inquiry -> inquiry.getImageUrl() == null));
    }

    @Test
    void send_withoutImage_savesWithNullUrl() {
        // given
        Inquiry saved = Inquiry.create(new Inquiry.CreateCommand(
                1L, InquiryType.INQUIRY, InquiryCategory.OTHER, "no image", null
        ));
        ReflectionTestUtils.setField(saved, "id", 3L);
        given(inquiryRepository.save(any(Inquiry.class))).willReturn(saved);
        given(inquiryProperties.getAdminEmail()).willReturn("admin@test.com");

        // when
        Long inquiryId = service.send(new SendInquiryUseCase.Command(
                1L, InquiryType.INQUIRY, InquiryCategory.OTHER, "no image", null
        ));

        // then
        assertThat(inquiryId).isEqualTo(3L);
        verify(imageStorage, never()).store(any());
        verify(inquiryRepository).save(argThat(inquiry -> inquiry.getImageUrl() == null));
    }

    @Test
    void send_dbFailure_deletesUploadedImage() throws Exception {
        // given
        MultipartFile image = mock(MultipartFile.class);
        given(image.isEmpty()).willReturn(false);
        given(image.getOriginalFilename()).willReturn("photo.jpg");
        given(image.getContentType()).willReturn("image/jpeg");
        given(image.getBytes()).willReturn(new byte[]{1, 2, 3});

        given(imageStorage.store(any())).willReturn(
                new MultipartImageStoragePort.StoredImage(
                        "images/1/inquiry/uuid.jpg",
                        com.example.freshkitchen.domain.image.enums.StorageProvider.S3,
                        "https://cdn.example.com/images/1/inquiry/uuid.jpg"
                )
        );
        given(inquiryRepository.save(any(Inquiry.class)))
                .willThrow(new RuntimeException("DB constraint violation"));

        // when & then
        try {
            service.send(new SendInquiryUseCase.Command(
                    1L, InquiryType.INQUIRY, InquiryCategory.RECIPE, "test", image
            ));
        } catch (RuntimeException e) {
            // expected
        }

        verify(imageStorage).delete(argThat(cmd ->
                cmd.objectKey().equals("images/1/inquiry/uuid.jpg")
        ));
    }
}
