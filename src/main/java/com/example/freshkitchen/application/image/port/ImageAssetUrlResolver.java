package com.example.freshkitchen.application.image.port;

import com.example.freshkitchen.domain.image.entity.ImageAsset;

public interface ImageAssetUrlResolver {

    String resolve(ImageAsset imageAsset);

    /**
     * 자산의 THUMBNAIL variant URL을 반환한다. 썸네일이 없으면 원본 URL로 폴백한다.
     */
    String resolveThumbnail(ImageAsset imageAsset);
}
