package com.example.freshkitchen.application.image.port;

import com.example.freshkitchen.domain.image.entity.ImageAsset;

public interface ImageAssetUrlResolver {

    String resolve(ImageAsset imageAsset);
}
