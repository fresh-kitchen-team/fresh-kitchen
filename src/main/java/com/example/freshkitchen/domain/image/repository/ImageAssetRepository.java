package com.example.freshkitchen.domain.image.repository;

import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.enums.AssetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ImageAssetRepository extends JpaRepository<ImageAsset, Long>, ImageAssetRepositoryCustom {

    /**
     * 어떤 식재료 이미지(IngredientImage)나 카탈로그 기본 이미지에도 연결되지 않은 고아(orphan) 자산을 찾는다.
     * 업로드 직후 첨부 직전의 자산이 잘못 삭제되지 않도록 cutoff(유예 기준 시각) 이전에 생성된 것만 대상으로 한다.
     */
    @Query("""
            select a from ImageAsset a
            where a.assetType = :assetType
              and a.createdAt < :cutoff
              and not exists (select 1 from IngredientImage ii where ii.imageAsset = a)
              and not exists (select 1 from IngredientCatalog c where c.defaultImageAsset = a)
            order by a.id asc
            """)
    List<ImageAsset> findOrphans(
            @Param("assetType") AssetType assetType,
            @Param("cutoff") OffsetDateTime cutoff,
            Pageable pageable
    );
}
