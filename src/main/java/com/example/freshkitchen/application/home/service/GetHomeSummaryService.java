package com.example.freshkitchen.application.home.service;

import com.example.freshkitchen.application.home.dto.HomeDto;
import com.example.freshkitchen.application.home.dto.HomeDto.HomeIngredientStatus;
import com.example.freshkitchen.application.home.usecase.GetHomeSummaryUseCase;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetHomeSummaryService implements GetHomeSummaryUseCase {

    private static final int PREVIEW_LIMIT = 5;
    private static final int NEAR_EXPIRY_DAYS = 7;
    private static final String DEFAULT_ITEM_EMOJI = "🍽️";
    private static final List<StorageDisplay> STORAGE_DISPLAYS = List.of(
            new StorageDisplay(StorageType.FRIDGE, "🥛", "냉장실", "fridge"),
            new StorageDisplay(StorageType.FREEZER, "❄️", "냉동실", "freezer"),
            new StorageDisplay(StorageType.PANTRY, "🥫", "팬트리", "pantry")
    );

    private final IngredientRepository ingredientRepository;
    private final Clock clock;

    @Override
    public HomeDto.SummaryResponse get(Query query) {
        validate(query);

        LocalDate today = LocalDate.now(clock);
        List<HomeIngredientSummary> activeIngredients = ingredientRepository
                .findAllByUserIdAndStatus(query.userId(), IngredientStatus.ACTIVE)
                .stream()
                .map(ingredient -> HomeIngredientSummary.from(
                        ingredient,
                        resolveStatus(ingredient, today),
                        resolveEmoji(ingredient)
                ))
                .toList();

        long expiredCount = countByStatus(activeIngredients, HomeIngredientStatus.EXPIRED);
        long nearExpiryCount = countByStatus(activeIngredients, HomeIngredientStatus.NEAR_EXPIRY);
        long freshCount = countByStatus(activeIngredients, HomeIngredientStatus.FRESH);

        return new HomeDto.SummaryResponse(
                activeIngredients.size(),
                freshCount,
                nearExpiryCount,
                expiredCount,
                storageSummaries(activeIngredients),
                previewItems(activeIngredients, HomeIngredientStatus.NEAR_EXPIRY, expiryAscending()),
                previewItems(activeIngredients, HomeIngredientStatus.EXPIRED, expiryAscending()),
                recentItems(activeIngredients)
        );
    }

    private static void validate(Query query) {
        if (query == null) {
            throw new BusinessValidationException("query must not be null");
        }
        if (query.userId() == null) {
            throw new BusinessValidationException("userId must not be null");
        }
        if (query.userId() <= 0) {
            throw new BusinessValidationException("userId must be positive");
        }
    }

    private long countByStatus(List<HomeIngredientSummary> ingredients, HomeIngredientStatus status) {
        return ingredients.stream()
                .filter(ingredient -> ingredient.status() == status)
                .count();
    }

    private List<HomeDto.StorageSummaryResponse> storageSummaries(List<HomeIngredientSummary> activeIngredients) {
        return STORAGE_DISPLAYS.stream()
                .map(display -> new HomeDto.StorageSummaryResponse(
                        display.storageType(),
                        display.emoji(),
                        display.name(),
                        countByStorage(activeIngredients, display.storageType()),
                        display.filterKey()
                ))
                .toList();
    }

    private long countByStorage(List<HomeIngredientSummary> ingredients, StorageType storageType) {
        return ingredients.stream()
                .filter(ingredient -> ingredient.storageType() == storageType)
                .count();
    }

    private List<HomeDto.ItemPreviewResponse> previewItems(
            List<HomeIngredientSummary> ingredients,
            HomeIngredientStatus status,
            Comparator<HomeIngredientSummary> comparator
    ) {
        return ingredients.stream()
                .filter(ingredient -> ingredient.status() == status)
                .sorted(comparator)
                .limit(PREVIEW_LIMIT)
                .map(HomeIngredientSummary::toPreview)
                .toList();
    }

    private List<HomeDto.ItemPreviewResponse> recentItems(List<HomeIngredientSummary> ingredients) {
        return ingredients.stream()
                .sorted(Comparator.comparing(
                                HomeIngredientSummary::createdAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HomeIngredientSummary::id, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(PREVIEW_LIMIT)
                .map(HomeIngredientSummary::toPreview)
                .toList();
    }

    private Comparator<HomeIngredientSummary> expiryAscending() {
        return Comparator.comparing(HomeIngredientSummary::expiresAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(HomeIngredientSummary::id, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private HomeIngredientStatus resolveStatus(Ingredient ingredient, LocalDate today) {
        LocalDate expiresAt = ingredient.getExpiresAt();
        if (expiresAt == null) {
            return HomeIngredientStatus.FRESH;
        }
        if (expiresAt.isBefore(today)) {
            return HomeIngredientStatus.EXPIRED;
        }
        if (!expiresAt.isAfter(today.plusDays(NEAR_EXPIRY_DAYS))) {
            return HomeIngredientStatus.NEAR_EXPIRY;
        }
        return HomeIngredientStatus.FRESH;
    }

    private String resolveEmoji(Ingredient ingredient) {
        IngredientCatalog catalog = ingredient.getCatalog();
        if (catalog == null || catalog.getEmoji() == null || catalog.getEmoji().isBlank()) {
            return DEFAULT_ITEM_EMOJI;
        }
        return catalog.getEmoji();
    }

    private record StorageDisplay(
            StorageType storageType,
            String emoji,
            String name,
            String filterKey
    ) {
    }

    private record HomeIngredientSummary(
            Long id,
            String name,
            StorageType storageType,
            LocalDate expiresAt,
            HomeIngredientStatus status,
            String emoji,
            OffsetDateTime createdAt
    ) {

        private static HomeIngredientSummary from(
                Ingredient ingredient,
                HomeIngredientStatus status,
                String emoji
        ) {
            return new HomeIngredientSummary(
                    ingredient.getId(),
                    ingredient.getName(),
                    ingredient.getStorage().getStorageType(),
                    ingredient.getExpiresAt(),
                    status,
                    emoji,
                    ingredient.getCreatedAt()
            );
        }

        private HomeDto.ItemPreviewResponse toPreview() {
            return new HomeDto.ItemPreviewResponse(
                    id,
                    name,
                    storageType,
                    expiresAt,
                    status,
                    emoji
            );
        }
    }
}
