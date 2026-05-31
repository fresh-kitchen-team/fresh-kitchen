package com.example.freshkitchen.application.analytics.service;

import com.example.freshkitchen.application.analytics.dto.AnalyticsDto;
import com.example.freshkitchen.application.analytics.usecase.ListExpiringItemsUseCase;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListExpiringItemsServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);

    private final IngredientRepository ingredientRepository = mock(IngredientRepository.class);
    private final ListExpiringItemsService service = new ListExpiringItemsService(ingredientRepository, CLOCK);

    @Test
    void list_returnsOnlyManualExpiryItems() {
        when(ingredientRepository.findAllByUserIdAndStatus(1L, IngredientStatus.ACTIVE)).thenReturn(List.of(
                ingredient("Manual", LocalDate.of(2026, 5, 2), ExpirySourceType.MANUAL),
                ingredient("Policy", LocalDate.of(2026, 5, 2), ExpirySourceType.POLICY),
                ingredient("Unknown", null, ExpirySourceType.UNKNOWN)
        ));

        List<AnalyticsDto.ExpiringItem> response = service.list(new ListExpiringItemsUseCase.Query(1L, 10, null));

        assertEquals(List.of("Manual"), response.stream().map(AnalyticsDto.ExpiringItem::name).toList());
    }

    private static Ingredient ingredient(String name, LocalDate expiresAt, ExpirySourceType expirySourceType) {
        User user = User.create(new User.CreateCommand("user-" + name, Provider.GOOGLE));
        Storage storage = Storage.create(new Storage.CreateCommand(user, StorageType.FRIDGE, "Fridge"));
        return Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                null,
                name,
                LocalDate.of(2026, 5, 1),
                expiresAt,
                expirySourceType,
                null,
                IngredientSourceType.MANUAL
        ));
    }
}
