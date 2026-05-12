package com.example.freshkitchen.application.home.service;

import com.example.freshkitchen.application.home.dto.HomeDto;
import com.example.freshkitchen.application.home.dto.HomeDto.HomeIngredientStatus;
import com.example.freshkitchen.application.home.usecase.GetHomeSummaryUseCase;
import com.example.freshkitchen.domain.catalog.entity.IngredientCatalog;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.entity.Storage;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.enums.Provider;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import({GetHomeSummaryService.class, GetHomeSummaryServiceTest.FixedClockConfig.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class GetHomeSummaryServiceTest extends PostgreSqlTestContainerSupport {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 12);

    private final GetHomeSummaryUseCase getHomeSummaryUseCase;

    @PersistenceContext
    private EntityManager entityManager;

    GetHomeSummaryServiceTest(GetHomeSummaryUseCase getHomeSummaryUseCase) {
        this.getHomeSummaryUseCase = getHomeSummaryUseCase;
    }

    @Test
    void get_countsActiveIngredientsAndBuildsHomePreview() {
        User owner = persistUser("home-owner", Provider.GOOGLE);
        User otherUser = persistUser("home-other", Provider.KAKAO);
        Storage fridge = persistStorage(owner, StorageType.FRIDGE, "Fridge");
        Storage freezer = persistStorage(owner, StorageType.FREEZER, "Freezer");
        Storage pantry = persistStorage(owner, StorageType.PANTRY, "Pantry");
        Storage otherFridge = persistStorage(otherUser, StorageType.FRIDGE, "Other fridge");
        IngredientCatalog eggCatalog = persistCatalog("Egg", CatalogCategory.DAIRY, StorageType.FRIDGE, "🥚");

        Ingredient expired = persistIngredient(owner, fridge, eggCatalog, "Expired egg", TODAY.minusDays(1));
        Ingredient nearToday = persistIngredient(owner, fridge, eggCatalog, "Egg", TODAY);
        Ingredient nearSevenDays = persistIngredient(owner, freezer, null, "Frozen dumpling", TODAY.plusDays(7));
        persistIngredient(owner, pantry, null, "Tuna can", null);
        persistIngredient(owner, fridge, null, "Fresh milk", TODAY.plusDays(8));
        Ingredient consumed = persistIngredient(owner, fridge, null, "Consumed onion", TODAY.plusDays(1));
        consumed.markConsumed(TODAY);
        persistIngredient(otherUser, otherFridge, null, "Other tomato", TODAY.minusDays(1));

        entityManager.flush();
        entityManager.clear();

        HomeDto.SummaryResponse response = getHomeSummaryUseCase.get(new GetHomeSummaryUseCase.Query(owner.getId()));

        assertAll(
                () -> assertEquals(5, response.totalCount()),
                () -> assertEquals(2, response.freshCount()),
                () -> assertEquals(2, response.nearExpiryCount()),
                () -> assertEquals(1, response.expiredCount()),
                () -> assertEquals(List.of(StorageType.FRIDGE, StorageType.FREEZER, StorageType.PANTRY),
                        response.storages().stream().map(HomeDto.StorageSummaryResponse::storage).toList()),
                () -> assertEquals(List.of(3L, 1L, 1L),
                        response.storages().stream().map(HomeDto.StorageSummaryResponse::itemCount).toList()),
                () -> assertEquals("냉장실", response.storages().get(0).name()),
                () -> assertEquals("fridge", response.storages().get(0).filterKey()),
                () -> assertEquals(List.of(nearToday.getId(), nearSevenDays.getId()),
                        response.nearExpiryItems().stream().map(HomeDto.ItemPreviewResponse::id).toList()),
                () -> assertEquals(List.of(expired.getId()),
                        response.expiredItems().stream().map(HomeDto.ItemPreviewResponse::id).toList()),
                () -> assertEquals(HomeIngredientStatus.NEAR_EXPIRY, response.nearExpiryItems().get(0).status()),
                () -> assertEquals("🥚", response.nearExpiryItems().get(0).emoji()),
                () -> assertEquals("🍽️", response.nearExpiryItems().get(1).emoji()),
                () -> assertEquals(5, response.recentItems().size())
        );
    }

    @Test
    void get_limitsPreviewItemsToFive() {
        User owner = persistUser("home-limit-owner", Provider.GOOGLE);
        Storage fridge = persistStorage(owner, StorageType.FRIDGE, "Fridge");

        for (int index = 0; index < 6; index++) {
            persistIngredient(owner, fridge, null, "Near " + index, TODAY.plusDays(index));
            persistIngredient(owner, fridge, null, "Expired " + index, TODAY.minusDays(index + 1L));
        }

        entityManager.flush();
        entityManager.clear();

        HomeDto.SummaryResponse response = getHomeSummaryUseCase.get(new GetHomeSummaryUseCase.Query(owner.getId()));

        assertAll(
                () -> assertEquals(5, response.nearExpiryItems().size()),
                () -> assertEquals(5, response.expiredItems().size()),
                () -> assertEquals(5, response.recentItems().size())
        );
    }

    @Test
    void get_withInvalidQuery_throwsBusinessValidationException() {
        assertAll(
                () -> assertThrows(BusinessValidationException.class, () -> getHomeSummaryUseCase.get(null)),
                () -> assertThrows(BusinessValidationException.class,
                        () -> getHomeSummaryUseCase.get(new GetHomeSummaryUseCase.Query(null))),
                () -> assertThrows(BusinessValidationException.class,
                        () -> getHomeSummaryUseCase.get(new GetHomeSummaryUseCase.Query(0L)))
        );
    }

    private User persistUser(String providerUserId, Provider provider) {
        User user = User.create(new User.CreateCommand(providerUserId, provider));
        entityManager.persist(user);
        return user;
    }

    private Storage persistStorage(User user, StorageType storageType, String name) {
        Storage storage = Storage.create(new Storage.CreateCommand(user, storageType, name));
        entityManager.persist(storage);
        return storage;
    }

    private Ingredient persistIngredient(
            User user,
            Storage storage,
            IngredientCatalog catalog,
            String name,
            LocalDate expiresAt
    ) {
        Ingredient ingredient = Ingredient.create(new Ingredient.CreateCommand(
                user,
                storage,
                catalog,
                name,
                null,
                expiresAt,
                ExpirySourceType.UNKNOWN,
                null,
                IngredientSourceType.MANUAL
        ));
        entityManager.persist(ingredient);
        return ingredient;
    }

    private IngredientCatalog persistCatalog(
            String name,
            CatalogCategory category,
            StorageType defaultStorageType,
            String iconUrl
    ) {
        IngredientCatalog catalog = IngredientCatalog.create(new IngredientCatalog.CreateCommand(
                null,
                name,
                category,
                defaultStorageType,
                iconUrl
        ));
        entityManager.persist(catalog);
        return catalog;
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        Clock clock() {
            return Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }
}
