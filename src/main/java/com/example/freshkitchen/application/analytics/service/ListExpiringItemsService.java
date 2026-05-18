package com.example.freshkitchen.application.analytics.service;

import com.example.freshkitchen.application.analytics.dto.AnalyticsDto;
import com.example.freshkitchen.application.analytics.dto.DisplayCategory;
import com.example.freshkitchen.application.analytics.usecase.ListExpiringItemsUseCase;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListExpiringItemsService implements ListExpiringItemsUseCase {

    private static final String DEFAULT_EMOJI = "🍽️";
    private static final int DEFAULT_MAX_D_DAY = 10;

    private final IngredientRepository ingredientRepository;
    private final Clock clock;

    @Override
    public List<AnalyticsDto.ExpiringItem> list(Query query) {
        LocalDate today = LocalDate.now(clock);
        int effectiveMaxDDay = query.maxDDay() != null ? query.maxDDay() : DEFAULT_MAX_D_DAY;
        LocalDate deadline = today.plusDays(effectiveMaxDDay);

        Stream<Ingredient> stream = ingredientRepository
                .findAllByUserIdAndStatus(query.userId(), IngredientStatus.ACTIVE)
                .stream()
                .filter(i -> i.getExpiresAt() != null && !i.getExpiresAt().isAfter(deadline));

        if (query.storageType() != null) {
            stream = stream.filter(i -> i.getStorage().getStorageType() == query.storageType());
        }

        return stream
                .sorted(Comparator.comparing(
                        Ingredient::getExpiresAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(i -> toExpiringItem(i, today))
                .toList();
    }

    private AnalyticsDto.ExpiringItem toExpiringItem(Ingredient ingredient, LocalDate today) {
        IngredientCatalog catalog = ingredient.getCatalog();
        CatalogCategory cc = catalog != null ? catalog.getCategory() : null;
        DisplayCategory dc = DisplayCategory.from(cc);
        String emoji = (catalog != null && catalog.getEmoji() != null && !catalog.getEmoji().isBlank())
                ? catalog.getEmoji() : DEFAULT_EMOJI;

        int dDay = ingredient.getExpiresAt() != null
                ? (int) ChronoUnit.DAYS.between(today, ingredient.getExpiresAt())
                : Integer.MAX_VALUE;

        return new AnalyticsDto.ExpiringItem(
                ingredient.getId(),
                ingredient.getName(),
                emoji,
                dc,
                dc.displayName(),
                ingredient.getExpiresAt(),
                dDay,
                ingredient.getStorage().getStorageType()
        );
    }
}
