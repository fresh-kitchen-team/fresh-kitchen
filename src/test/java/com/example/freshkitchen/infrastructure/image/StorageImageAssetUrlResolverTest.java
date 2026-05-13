package com.example.freshkitchen.infrastructure.image;

import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.enums.ImageKind;
import com.example.freshkitchen.domain.image.enums.StorageProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageImageAssetUrlResolverTest {

    @Test
    void resolve_returnsLocalPublicUrl() {
        LocalImageStorageProperties localProperties = new LocalImageStorageProperties();
        localProperties.setPublicBaseUrl("/uploads/");
        StorageImageAssetUrlResolver resolver = new StorageImageAssetUrlResolver(
                localProperties,
                new DefaultListableBeanFactory().getBeanProvider(S3ImageStorageProperties.class)
        );

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
        StorageImageAssetUrlResolver resolver = new StorageImageAssetUrlResolver(
                localProperties,
                beanFactory.getBeanProvider(S3ImageStorageProperties.class)
        );

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
        StorageImageAssetUrlResolver resolver = new StorageImageAssetUrlResolver(
                localProperties,
                beanFactory.getBeanProvider(S3ImageStorageProperties.class)
        );

        String imageUrl = resolver.resolve(imageAsset(StorageProvider.S3, "images/1/ingredient/tomato.jpg"));

        assertEquals("https://freshkitchen-images.s3.ap-northeast-2.amazonaws.com/images/1/ingredient/tomato.jpg",
                imageUrl);
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
