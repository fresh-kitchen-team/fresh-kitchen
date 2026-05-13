package com.example.freshkitchen.infrastructure.image;

import com.example.freshkitchen.application.image.port.ImageAssetUrlResolver;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageImageAssetUrlResolver implements ImageAssetUrlResolver {

    private final LocalImageStorageProperties localImageStorageProperties;
    private final ObjectProvider<S3ImageStorageProperties> s3ImageStorageProperties;

    @Override
    public String resolve(ImageAsset imageAsset) {
        if (imageAsset == null) {
            throw new BusinessValidationException("imageAsset must not be null");
        }
        if (imageAsset.getStorageProvider() == StorageProvider.LOCAL) {
            return withBaseUrl(localImageStorageProperties.getPublicBaseUrl(), imageAsset.getObjectKey());
        }
        if (imageAsset.getStorageProvider() == StorageProvider.S3) {
            S3ImageStorageProperties properties = s3ImageStorageProperties.getIfAvailable();
            if (properties == null) {
                throw new BusinessValidationException("s3 image storage properties must be configured");
            }
            String publicBaseUrl = properties.getPublicBaseUrl();
            if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
                return withBaseUrl(publicBaseUrl, imageAsset.getObjectKey());
            }
            return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                    properties.getBucket(),
                    properties.getRegion(),
                    imageAsset.getObjectKey()
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
