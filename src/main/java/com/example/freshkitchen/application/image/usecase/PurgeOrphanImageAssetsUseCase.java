package com.example.freshkitchen.application.image.usecase;

import java.time.OffsetDateTime;

/**
 * 어떤 식재료 이미지에도 연결되지 않은 고아(orphan) ImageAsset과 그 variant·스토리지 객체를 정리한다.
 * IngredientImage join 레코드만 삭제하는 제거 흐름(RemoveIngredientImage)에서 분리되어 남은 원본을 거버넌스 단계에서 청소한다.
 */
public interface PurgeOrphanImageAssetsUseCase {

    Result purge(Command command);

    /**
     * @param cutoff    이 시각 이전에 생성된 자산만 정리 대상 (업로드 직후 첨부 전 자산 보호용 유예 기준)
     * @param batchSize 한 번의 실행에서 정리할 최대 자산 수
     */
    record Command(OffsetDateTime cutoff, int batchSize) {
    }

    record Result(int deletedAssets, int deletedVariants, int deletedStorageObjects) {
    }
}
