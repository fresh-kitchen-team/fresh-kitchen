package com.example.freshkitchen.application.image.service;

import com.example.freshkitchen.application.image.port.MultipartImageStoragePort;
import com.example.freshkitchen.application.image.usecase.PurgeOrphanImageAssetsUseCase;
import com.example.freshkitchen.domain.image.entity.ImageAsset;
import com.example.freshkitchen.domain.image.entity.ImageVariant;
import com.example.freshkitchen.domain.image.enums.AssetType;
import com.example.freshkitchen.domain.image.repository.ImageAssetRepository;
import com.example.freshkitchen.domain.image.repository.ImageVariantRepository;
import com.example.freshkitchen.domain.image.repository.IngredientImageRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurgeOrphanImageAssetsService implements PurgeOrphanImageAssetsUseCase {

    private final ImageAssetRepository imageAssetRepository;
    private final ImageVariantRepository imageVariantRepository;
    private final IngredientImageRepository ingredientImageRepository;
    private final MultipartImageStoragePort multipartImageStoragePort;
    private final TransactionOperations transactionOperations;

    @Override
    public Result purge(Command command) {
        validate(command);

        List<ImageAsset> orphans = imageAssetRepository.findOrphans(
                AssetType.USER_UPLOAD,
                command.cutoff(),
                PageRequest.of(0, command.batchSize())
        );

        int deletedAssets = 0;
        int deletedVariants = 0;
        int deletedStorageObjects = 0;

        for (ImageAsset orphan : orphans) {
            // DB 삭제는 트랜잭션 내에서, 스토리지 삭제는 커밋 이후에 수행한다(외부 호출을 트랜잭션 밖으로).
            PurgedImage purged = transactionOperations.execute(status -> deleteOne(orphan.getId()));
            if (purged == null) {
                continue; // 조회와 삭제 사이에 참조가 다시 생겼거나 이미 삭제됨
            }
            deletedAssets++;
            deletedVariants += purged.variantObjectKeys().size();
            deletedStorageObjects += deleteStorageObjects(purged);
        }

        return new Result(deletedAssets, deletedVariants, deletedStorageObjects);
    }

    private PurgedImage deleteOne(Long imageAssetId) {
        ImageAsset asset = imageAssetRepository.findById(imageAssetId).orElse(null);
        if (asset == null || ingredientImageRepository.existsByImageAssetId(imageAssetId)) {
            return null;
        }

        List<String> variantObjectKeys = imageVariantRepository.findAllByImageAssetIdOrderByIdAsc(imageAssetId)
                .stream()
                .map(ImageVariant::getObjectKey)
                .toList();
        String assetObjectKey = asset.getObjectKey();

        imageVariantRepository.deleteByImageAssetId(imageAssetId);
        imageAssetRepository.delete(asset);

        return new PurgedImage(assetObjectKey, variantObjectKeys);
    }

    private int deleteStorageObjects(PurgedImage purged) {
        int deleted = deleteQuietly(purged.assetObjectKey());
        for (String variantObjectKey : purged.variantObjectKeys()) {
            deleted += deleteQuietly(variantObjectKey);
        }
        return deleted;
    }

    private int deleteQuietly(String objectKey) {
        try {
            multipartImageStoragePort.delete(new MultipartImageStoragePort.DeleteCommand(objectKey));
            return 1;
        } catch (Exception e) {
            // DB 행은 이미 삭제됐으므로 스토리지 객체만 남는다. 다음 회차에 재시도되지 않으니 로그로만 남긴다.
            log.warn("[OrphanPurge] storage delete failed objectKey={}", objectKey, e);
            return 0;
        }
    }

    private static void validate(Command command) {
        if (command == null) {
            throw new BusinessValidationException("command must not be null");
        }
        if (command.cutoff() == null) {
            throw new BusinessValidationException("cutoff must not be null");
        }
        if (command.batchSize() <= 0) {
            throw new BusinessValidationException("batchSize must be positive");
        }
    }

    private record PurgedImage(String assetObjectKey, List<String> variantObjectKeys) {
    }
}
