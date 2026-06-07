package com.example.freshkitchen.application.image.scheduler;

import com.example.freshkitchen.application.image.usecase.PurgeOrphanImageAssetsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "image.orphan-purge", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrphanImageAssetPurgeScheduler {

    private final PurgeOrphanImageAssetsUseCase purgeOrphanImageAssetsUseCase;

    @Value("${image.orphan-purge.grace-period}")
    private Duration gracePeriod;

    @Value("${image.orphan-purge.batch-size}")
    private int batchSize;

    @Scheduled(
            cron = "${image.orphan-purge.cron}",
            zone = "${image.orphan-purge.zone}"
    )
    public void run() {
        try {
            OffsetDateTime cutoff = OffsetDateTime.now().minus(gracePeriod);
            PurgeOrphanImageAssetsUseCase.Result result = purgeOrphanImageAssetsUseCase.purge(
                    new PurgeOrphanImageAssetsUseCase.Command(cutoff, batchSize)
            );
            log.info("[OrphanPurge] deletedAssets={} deletedVariants={} deletedStorageObjects={}",
                    result.deletedAssets(), result.deletedVariants(), result.deletedStorageObjects());
        } catch (Exception e) {
            log.error("[OrphanPurge] scheduler failed", e);
        }
    }
}
