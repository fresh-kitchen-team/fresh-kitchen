package com.example.freshkitchen.application.analytics.service;

import com.example.freshkitchen.application.analytics.dto.AnalyticsDto;
import com.example.freshkitchen.application.analytics.dto.DisplayCategory;
import com.example.freshkitchen.application.analytics.usecase.GetAnalyticsSummaryUseCase;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAnalyticsSummaryService implements GetAnalyticsSummaryUseCase {

    private static final int URGENT_DAYS = 3;
    private static final int URGENT_ITEMS_LIMIT = 10;
    private static final String DEFAULT_EMOJI = "🍽️";

    private final IngredientRepository ingredientRepository;
    private final Clock clock;

    @Override
    public AnalyticsDto.SummaryResponse get(Query query) {
        validate(query);

        LocalDate today = LocalDate.now(clock);
        List<Ingredient> activeIngredients = ingredientRepository
                .findAllByUserIdAndStatus(query.userId(), IngredientStatus.ACTIVE);
        List<Ingredient> consumedIngredients = ingredientRepository
                .findByUserIdAndStatus(query.userId(), IngredientStatus.CONSUMED);
        List<Ingredient> discardedIngredients = ingredientRepository
                .findByUserIdAndStatus(query.userId(), IngredientStatus.DISCARDED);

        Map<DisplayCategory, int[]> categoryMap = buildCategoryMap(activeIngredients, today);
        Map<DisplayCategory, int[]> discardMap = buildDiscardMap(consumedIngredients, discardedIngredients);
        List<AnalyticsDto.CategoryStat> categoryStats = buildCategoryStats(categoryMap, discardMap);

        List<AnalyticsDto.UrgentItem> urgentItems = activeIngredients.stream()
                .filter(i -> isUrgent(i, today))
                .sorted(Comparator.comparing(Ingredient::getExpiresAt))
                .limit(URGENT_ITEMS_LIMIT)
                .map(i -> toUrgentItem(i, today))
                .toList();

        int totalActive = activeIngredients.size();
        int urgentCount = (int) activeIngredients.stream().filter(i -> isUrgent(i, today)).count();

        DisplayCategory topCategory = null;
        int topCategoryCount = 0;
        for (var stat : categoryStats) {
            if (stat.urgentCount() > topCategoryCount) {
                topCategoryCount = stat.urgentCount();
                topCategory = stat.category();
            }
        }

        String message = buildMessage(urgentCount, topCategory, topCategoryCount);

        int totalConsumed = consumedIngredients.size();
        int totalDiscarded = discardedIngredients.size();
        double overallDiscardRate = calculateDiscardRate(totalConsumed, totalDiscarded);

        return new AnalyticsDto.SummaryResponse(
                totalActive,
                urgentCount,
                topCategory,
                topCategory != null ? topCategory.displayName() : null,
                topCategoryCount,
                message,
                overallDiscardRate,
                categoryStats,
                urgentItems
        );
    }

    private static void validate(Query query) {
        if (query == null) {
            throw new BusinessValidationException("query must not be null");
        }
        if (query.userId() == null || query.userId() <= 0) {
            throw new BusinessValidationException("userId must be positive");
        }
    }

    private boolean isUrgent(Ingredient ingredient, LocalDate today) {
        LocalDate expiresAt = ingredient.getExpiresAt();
        if (expiresAt == null) {
            return false;
        }
        return !expiresAt.isAfter(today.plusDays(URGENT_DAYS));
    }

    private Map<DisplayCategory, int[]> buildCategoryMap(List<Ingredient> ingredients, LocalDate today) {
        Map<DisplayCategory, int[]> map = new EnumMap<>(DisplayCategory.class);
        for (DisplayCategory dc : DisplayCategory.values()) {
            map.put(dc, new int[]{0, 0}); // [activeCount, urgentCount]
        }

        for (Ingredient ingredient : ingredients) {
            CatalogCategory cc = ingredient.getCatalog() != null
                    ? ingredient.getCatalog().getCategory()
                    : null;
            DisplayCategory dc = DisplayCategory.from(cc);
            int[] counts = map.get(dc);
            counts[0]++;
            if (isUrgent(ingredient, today)) {
                counts[1]++;
            }
        }
        return map;
    }

    private Map<DisplayCategory, int[]> buildDiscardMap(List<Ingredient> consumed, List<Ingredient> discarded) {
        Map<DisplayCategory, int[]> map = new EnumMap<>(DisplayCategory.class);
        for (DisplayCategory dc : DisplayCategory.values()) {
            map.put(dc, new int[]{0, 0}); // [consumedCount, discardedCount]
        }
        for (Ingredient i : consumed) {
            DisplayCategory dc = resolveDisplayCategory(i);
            map.get(dc)[0]++;
        }
        for (Ingredient i : discarded) {
            DisplayCategory dc = resolveDisplayCategory(i);
            map.get(dc)[1]++;
        }
        return map;
    }

    private static DisplayCategory resolveDisplayCategory(Ingredient ingredient) {
        CatalogCategory cc = ingredient.getCatalog() != null
                ? ingredient.getCatalog().getCategory() : null;
        return DisplayCategory.from(cc);
    }

    private static double calculateDiscardRate(int consumed, int discarded) {
        int total = consumed + discarded;
        if (total == 0) return 0.0;
        return Math.round(((double) discarded / total) * 1000.0) / 10.0; // 소수점 1자리
    }

    private List<AnalyticsDto.CategoryStat> buildCategoryStats(
            Map<DisplayCategory, int[]> categoryMap,
            Map<DisplayCategory, int[]> discardMap) {
        return java.util.Arrays.stream(DisplayCategory.values())
                .map(dc -> {
                    int[] counts = categoryMap.get(dc);
                    int[] discard = discardMap.get(dc);
                    double rate = calculateDiscardRate(discard[0], discard[1]);
                    return new AnalyticsDto.CategoryStat(dc, dc.displayName(), counts[0], counts[1], rate);
                })
                .toList();
    }

    private AnalyticsDto.UrgentItem toUrgentItem(Ingredient ingredient, LocalDate today) {
        IngredientCatalog catalog = ingredient.getCatalog();
        String emoji = (catalog != null && catalog.getEmoji() != null && !catalog.getEmoji().isBlank())
                ? catalog.getEmoji()
                : DEFAULT_EMOJI;

        int dDay = (int) ChronoUnit.DAYS.between(today, ingredient.getExpiresAt());

        return new AnalyticsDto.UrgentItem(
                ingredient.getId(),
                ingredient.getName(),
                emoji,
                ingredient.getExpiresAt(),
                dDay,
                ingredient.getStorage().getStorageType()
        );
    }

    private String buildMessage(int urgentCount, DisplayCategory topCategory, int topCategoryCount) {
        if (urgentCount == 0) {
            return "유통기한이 임박한 식재료가 없습니다. 잘 관리하고 계시네요!";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("유통기한이 3일 이내인 식재료가 ").append(urgentCount).append("개 있습니다.");

        if (topCategory != null && topCategoryCount > 0) {
            sb.append(" 가장 주의가 필요한 카테고리는 ")
                    .append(topCategory.displayName())
                    .append("(").append(topCategoryCount).append("개)입니다.");
        }

        return sb.toString();
    }
}
