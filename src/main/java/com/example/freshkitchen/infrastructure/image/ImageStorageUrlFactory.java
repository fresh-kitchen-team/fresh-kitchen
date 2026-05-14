package com.example.freshkitchen.infrastructure.image;

import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageStorageUrlFactory {

    private final LocalImageStorageProperties localImageStorageProperties;
    private final ObjectProvider<S3ImageStorageProperties> s3ImageStorageProperties;

    public String create(StorageProvider storageProvider, String objectKey) {
        if (storageProvider == StorageProvider.LOCAL) {
            return withBaseUrl(localImageStorageProperties.getPublicBaseUrl(), objectKey);
        }
        if (storageProvider == StorageProvider.S3) {
            S3ImageStorageProperties properties = s3ImageStorageProperties.getIfAvailable();
            if (properties == null) {
                throw new BusinessValidationException("s3 image storage properties must be configured");
            }
            String publicBaseUrl = properties.getPublicBaseUrl();
            if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
                return withBaseUrl(publicBaseUrl, objectKey);
            }
            return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                    properties.getBucket(),
                    properties.getRegion(),
                    objectKey
            );
        }
        throw new BusinessValidationException("storageProvider must be supported image storage provider");
    }

    private static String withBaseUrl(String baseUrl, String objectKey) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedObjectKey = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        return normalizedBaseUrl + "/" + normalizedObjectKey;
    }
}
