package com.example.freshkitchen.application.home.service;

import com.example.freshkitchen.application.home.dto.HomeDto;
import com.example.freshkitchen.application.home.dto.HomeDto.HomeIngredientStatus;
import com.example.freshkitchen.application.home.usecase.GetHomeSummaryUseCase;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public HomeDto.SummaryResponse get(Query query) {
        LocalDate today = LocalDate.now();
        List<Ingredient> activeIngredients = ingredientRepository.findAllByUserId(query.userId()).stream()
                .filter(ingredient -> ingredient.getStatus() == IngredientStatus.ACTIVE)
                .toList();

        long expiredCount = countByStatus(activeIngredients, today, HomeIngredientStatus.EXPIRED);
        long nearExpiryCount = countByStatus(activeIngredients, today, HomeIngredientStatus.NEAR_EXPIRY);
        long freshCount = countByStatus(activeIngredients, today, HomeIngredientStatus.FRESH);

        return new HomeDto.SummaryResponse(
                activeIngredients.size(),
                freshCount,
                nearExpiryCount,
                expiredCount,
                storageSummaries(activeIngredients),
                previewItems(activeIngredients, today, HomeIngredientStatus.NEAR_EXPIRY, expiryAscending()),
                previewItems(activeIngredients, today, HomeIngredientStatus.EXPIRED, expiryAscending()),
                recentItems(activeIngredients, today)
        );
    }

    private long countByStatus(List<Ingredient> ingredients, LocalDate today, HomeIngredientStatus status) {
        return ingredients.stream()
                .filter(ingredient -> resolveStatus(ingredient, today) == status)
                .count();
    }

    private List<HomeDto.StorageSummaryResponse> storageSummaries(List<Ingredient> activeIngredients) {
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

    private long countByStorage(List<Ingredient> ingredients, StorageType storageType) {
        return ingredients.stream()
                .filter(ingredient -> ingredient.getStorage().getStorageType() == storageType)
                .count();
    }

    private List<HomeDto.ItemPreviewResponse> previewItems(
            List<Ingredient> ingredients,
            LocalDate today,
            HomeIngredientStatus status,
            Comparator<Ingredient> comparator
    ) {
        return ingredients.stream()
                .filter(ingredient -> resolveStatus(ingredient, today) == status)
                .sorted(comparator)
                .limit(PREVIEW_LIMIT)
                .map(ingredient -> toPreview(ingredient, today))
                .toList();
    }

    private List<HomeDto.ItemPreviewResponse> recentItems(List<Ingredient> ingredients, LocalDate today) {
        return ingredients.stream()
                .sorted(Comparator.comparing(Ingredient::getCreatedAt, nullsLastOffsetDateTime()).reversed()
                        .thenComparing(Ingredient::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(PREVIEW_LIMIT)
                .map(ingredient -> toPreview(ingredient, today))
                .toList();
    }

    private Comparator<Ingredient> expiryAscending() {
        return Comparator.comparing(Ingredient::getExpiresAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Ingredient::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private Comparator<OffsetDateTime> nullsLastOffsetDateTime() {
        return Comparator.nullsLast(Comparator.naturalOrder());
    }

    private HomeDto.ItemPreviewResponse toPreview(Ingredient ingredient, LocalDate today) {
        return new HomeDto.ItemPreviewResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getStorage().getStorageType(),
                ingredient.getExpiresAt(),
                resolveStatus(ingredient, today),
                resolveEmoji(ingredient)
        );
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
        if (catalog == null || catalog.getIconUrl() == null || catalog.getIconUrl().isBlank()) {
            return DEFAULT_ITEM_EMOJI;
        }
        return catalog.getIconUrl();
    }

    private record StorageDisplay(
            StorageType storageType,
            String emoji,
            String name,
            String filterKey
    ) {
    }
}
