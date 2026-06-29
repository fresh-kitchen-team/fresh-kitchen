package com.example.freshkitchen.infrastructure.image;

import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.ImageVariant;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.ImageVariantType;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageImageAssetUrlResolverTest {

    @Test
    void resolve_returnsLocalPublicUrl() {
        LocalImageStorageProperties localProperties = new LocalImageStorageProperties();
        localProperties.setPublicBaseUrl("/uploads/");
        ImageStorageUrlFactory imageStorageUrlFactory = new ImageStorageUrlFactory(
                localProperties,
                new DefaultListableBeanFactory().getBeanProvider(S3ImageStorageProperties.class)
        );
        StorageImageAssetUrlResolver resolver = new StorageImageAssetUrlResolver(imageStorageUrlFactory);

        String imageUrl = resolver.resolve(imageAsset(StorageProvider.LOCAL, "images/1/ingredient/tomato.jpg"));

        assertEquals("/uploads/images/1/ingredient/tomato.jpg", imageUrl);
    }

    @Test
    void resolve_returnsS3PublicUrlFromConfiguredBaseUrl() {
        LocalImageStorageProperties localProperties = new LocalImageStorageProperties();
        S3ImageStorageProperties s3Properties = new S3ImageStorageProperties();
        s3Properties.setPublicBaseUrl("https://cdn.example.com/");
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("s3ImageStorageProperties", s3Properties);
        ImageStorageUrlFactory imageStorageUrlFactory = new ImageStorageUrlFactory(
                localProperties,
                beanFactory.getBeanProvider(S3ImageStorageProperties.class)
        );
        StorageImageAssetUrlResolver resolver = new StorageImageAssetUrlResolver(imageStorageUrlFactory);

        String imageUrl = resolver.resolve(imageAsset(StorageProvider.S3, "images/1/ingredient/tomato.jpg"));

        assertEquals("https://cdn.example.com/images/1/ingredient/tomato.jpg", imageUrl);
    }

    @Test
    void resolve_returnsDefaultS3UrlWhenPublicBaseUrlIsMissing() {
        LocalImageStorageProperties localProperties = new LocalImageStorageProperties();
        S3ImageStorageProperties s3Properties = new S3ImageStorageProperties();
        s3Properties.setRegion("ap-northeast-2");
        s3Properties.setBucket("freshkitchen-images");
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("s3ImageStorageProperties", s3Properties);
        ImageStorageUrlFactory imageStorageUrlFactory = new ImageStorageUrlFactory(
                localProperties,
                beanFactory.getBeanProvider(S3ImageStorageProperties.class)
        );
        StorageImageAssetUrlResolver resolver = new StorageImageAssetUrlResolver(imageStorageUrlFactory);

        String imageUrl = resolver.resolve(imageAsset(StorageProvider.S3, "images/1/ingredient/tomato.jpg"));

        assertEquals("https://freshkitchen-images.s3.ap-northeast-2.amazonaws.com/images/1/ingredient/tomato.jpg",
                imageUrl);
    }

    @Test
    void resolveThumbnail_returnsThumbnailVariantUrl_whenThumbnailExists() {
        StorageImageAssetUrlResolver resolver = localResolver();
        ImageAsset asset = imageAsset(StorageProvider.LOCAL, "images/1/ingredient/tomato.jpg");
        asset.getVariants().add(ImageVariant.create(new ImageVariant.CreateCommand(
                asset, ImageVariantType.THUMBNAIL, "images/1/ingredient/tomato_thumb.jpg", 320, 240)));

        String thumbnailUrl = resolver.resolveThumbnail(asset);

        assertEquals("/uploads/images/1/ingredient/tomato_thumb.jpg", thumbnailUrl);
    }

    @Test
    void resolveThumbnail_fallsBackToOriginalUrl_whenNoThumbnailVariant() {
        StorageImageAssetUrlResolver resolver = localResolver();
        ImageAsset asset = imageAsset(StorageProvider.LOCAL, "images/1/ingredient/tomato.jpg");

        String thumbnailUrl = resolver.resolveThumbnail(asset);

        assertEquals("/uploads/images/1/ingredient/tomato.jpg", thumbnailUrl);
    }

    private static StorageImageAssetUrlResolver localResolver() {
        LocalImageStorageProperties localProperties = new LocalImageStorageProperties();
        localProperties.setPublicBaseUrl("/uploads/");
        ImageStorageUrlFactory imageStorageUrlFactory = new ImageStorageUrlFactory(
                localProperties,
                new DefaultListableBeanFactory().getBeanProvider(S3ImageStorageProperties.class)
        );
        return new StorageImageAssetUrlResolver(imageStorageUrlFactory);
    }

    private static ImageAsset imageAsset(StorageProvider storageProvider, String objectKey) {
        return ImageAsset.create(new ImageAsset.CreateCommand(
                null,
                AssetType.SYSTEM_DEFAULT,
                ImageKind.INGREDIENT,
                storageProvider,
                objectKey,
                300,
                300
        ));
    }
}
