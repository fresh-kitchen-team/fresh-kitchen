package com.example.freshkitchen.infrastructure.image;

import com.example.freshkitchen.application.image.port.ImageAssetUrlResolver;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.enums.ImageVariantType;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageImageAssetUrlResolver implements ImageAssetUrlResolver {

    private final ImageStorageUrlFactory imageStorageUrlFactory;

    @Override
    public String resolve(ImageAsset imageAsset) {
        if (imageAsset == null) {
            throw new BusinessValidationException("imageAsset must not be null");
        }
        return imageStorageUrlFactory.create(imageAsset.getStorageProvider(), imageAsset.getObjectKey());
    }

    @Override
    public String resolveThumbnail(ImageAsset imageAsset) {
        if (imageAsset == null) {
            throw new BusinessValidationException("imageAsset must not be null");
        }
        return imageAsset.getVariants().stream()
                .filter(variant -> variant.getVariantType() == ImageVariantType.THUMBNAIL)
                .findFirst()
                .map(variant -> imageStorageUrlFactory.create(
                        imageAsset.getStorageProvider(), variant.getObjectKey()))
                .orElseGet(() -> resolve(imageAsset));
    }
}
