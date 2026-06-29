package com.example.freshkitchen.presentation.item;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.ConsumeIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientWithImageUseCase;
import com.example.freshkitchen.application.ingredient.usecase.DeleteIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.GetIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ListIngredientsUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ListStoragesUseCase;
import com.example.freshkitchen.application.ingredient.usecase.UpdateIngredientUseCase;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.global.exception.handler.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemControllerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);

    private ConsumeIngredientUseCase consumeIngredientUseCase;
    private CreateIngredientWithImageUseCase createIngredientWithImageUseCase;
    private UpdateIngredientUseCase updateIngredientUseCase;
    private GetIngredientUseCase getIngredientUseCase;
    private ListIngredientsUseCase listIngredientsUseCase;
    private DeleteIngredientUseCase deleteIngredientUseCase;
    private ListStoragesUseCase listStoragesUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        consumeIngredientUseCase = mock(ConsumeIngredientUseCase.class);
        createIngredientWithImageUseCase = mock(CreateIngredientWithImageUseCase.class);
        updateIngredientUseCase = mock(UpdateIngredientUseCase.class);
        getIngredientUseCase = mock(GetIngredientUseCase.class);
        listIngredientsUseCase = mock(ListIngredientsUseCase.class);
        deleteIngredientUseCase = mock(DeleteIngredientUseCase.class);
        listStoragesUseCase = mock(ListStoragesUseCase.class);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ItemController controller = new ItemController(
                consumeIngredientUseCase,
                createIngredientWithImageUseCase,
                updateIngredientUseCase,
                getIngredientUseCase,
                listIngredientsUseCase,
                deleteIngredientUseCase,
                listStoragesUseCase,
                CLOCK
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void create_returnsCreatedItemIdAndAttachesScanImage() throws Exception {
        when(createIngredientWithImageUseCase.create(any(CreateIngredientWithImageUseCase.Command.class))).thenReturn(10L);

        mockMvc.perform(post("/api/v1/items")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tomato",
                                  "storageType": "FRIDGE",
                                  "category": "GRAIN",
                                  "expiryDate": "2026-05-06",
                                  "purchaseDate": "2026-04-29",
                                  "memo": "salad",
                                  "imageAssetId": 20
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value(10));

        ArgumentCaptor<CreateIngredientWithImageUseCase.Command> createCaptor =
                ArgumentCaptor.forClass(CreateIngredientWithImageUseCase.Command.class);
        verify(createIngredientWithImageUseCase).create(createCaptor.capture());
        assertAll(
                () -> assertEquals(1L, createCaptor.getValue().ingredientCommand().userId()),
                () -> assertEquals(StorageType.FRIDGE, createCaptor.getValue().ingredientCommand().storageType()),
                () -> assertNull(createCaptor.getValue().ingredientCommand().catalogId()),
                () -> assertEquals("Tomato", createCaptor.getValue().ingredientCommand().name()),
                () -> assertEquals(LocalDate.of(2026, 4, 29), createCaptor.getValue().ingredientCommand().registeredAt()),
                () -> assertEquals(LocalDate.of(2026, 5, 6), createCaptor.getValue().ingredientCommand().expiresAt()),
                () -> assertEquals(ExpirySourceType.MANUAL, createCaptor.getValue().ingredientCommand().expirySourceType()),
                () -> assertEquals("salad", createCaptor.getValue().ingredientCommand().note()),
                () -> assertEquals(IngredientSourceType.MANUAL, createCaptor.getValue().ingredientCommand().sourceType()),
                () -> assertEquals(20L, createCaptor.getValue().imageAssetId())
        );
    }

    @Test
    void create_defaultsPurchaseDateToToday() throws Exception {
        when(createIngredientWithImageUseCase.create(any(CreateIngredientWithImageUseCase.Command.class))).thenReturn(10L);

        mockMvc.perform(post("/api/v1/items")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tomato",
                                  "storageType": "FRIDGE"
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateIngredientWithImageUseCase.Command> captor =
                ArgumentCaptor.forClass(CreateIngredientWithImageUseCase.Command.class);
        verify(createIngredientWithImageUseCase).create(captor.capture());
        assertEquals(LocalDate.of(2026, 5, 1), captor.getValue().ingredientCommand().registeredAt());
    }

    @Test
    void create_mapsPhotoSourceType() throws Exception {
        when(createIngredientWithImageUseCase.create(any(CreateIngredientWithImageUseCase.Command.class))).thenReturn(10L);

        mockMvc.perform(post("/api/v1/items")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tomato",
                                  "storageType": "FRIDGE",
                                  "sourceType": "PHOTO"
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateIngredientWithImageUseCase.Command> captor =
                ArgumentCaptor.forClass(CreateIngredientWithImageUseCase.Command.class);
        verify(createIngredientWithImageUseCase).create(captor.capture());
        assertEquals(IngredientSourceType.PHOTO, captor.getValue().ingredientCommand().sourceType());
    }

    @Test
    void create_mapsReceiptSourceType() throws Exception {
        when(createIngredientWithImageUseCase.create(any(CreateIngredientWithImageUseCase.Command.class))).thenReturn(10L);

        mockMvc.perform(post("/api/v1/items")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tomato",
                                  "storageType": "FRIDGE",
                                  "sourceType": "RECEIPT"
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateIngredientWithImageUseCase.Command> captor =
                ArgumentCaptor.forClass(CreateIngredientWithImageUseCase.Command.class);
        verify(createIngredientWithImageUseCase).create(captor.capture());
        assertEquals(IngredientSourceType.RECEIPT, captor.getValue().ingredientCommand().sourceType());
    }

    @Test
    void list_returnsItemSummaries() throws Exception {
        when(listIngredientsUseCase.list(any(ListIngredientsUseCase.Query.class))).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/v1/items")
                        .principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].name").value("Tomato"))
                .andExpect(jsonPath("$.data[0].status").value("NEAR_EXPIRY"))
                .andExpect(jsonPath("$.data[0].catalogId").value(3))
                .andExpect(jsonPath("$.data[0].storageId").value(2))
                .andExpect(jsonPath("$.data[0].storage").value("FRIDGE"))
                .andExpect(jsonPath("$.data[0].category").value("VEGETABLE"))
                .andExpect(jsonPath("$.data[0].expiryDate").value("2026-05-06"))
                .andExpect(jsonPath("$.data[0].emoji").value("🍅"))
                .andExpect(jsonPath("$.data[0].purchaseDate").value("2026-04-29"))
                .andExpect(jsonPath("$.data[0].memo").value("salad"));

        ArgumentCaptor<ListIngredientsUseCase.Query> captor =
                ArgumentCaptor.forClass(ListIngredientsUseCase.Query.class);
        verify(listIngredientsUseCase).list(captor.capture());
        assertEquals(1L, captor.getValue().userId());
    }

    @Test
    void get_returnsItemDetail() throws Exception {
        when(getIngredientUseCase.get(any(GetIngredientUseCase.Query.class))).thenReturn(detail());

        mockMvc.perform(get("/api/v1/items/10")
                        .principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("NEAR_EXPIRY"));

        ArgumentCaptor<GetIngredientUseCase.Query> captor =
                ArgumentCaptor.forClass(GetIngredientUseCase.Query.class);
        verify(getIngredientUseCase).get(captor.capture());
        assertAll(
                () -> assertEquals(10L, captor.getValue().ingredientId()),
                () -> assertEquals(1L, captor.getValue().userId())
        );
    }

    @Test
    void get_returnsNotFoundForDiscardedItem() throws Exception {
        when(getIngredientUseCase.get(any(GetIngredientUseCase.Query.class)))
                .thenThrow(new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/items/10")
                        .principal(auth(1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/v1/items/10"));
    }

    @Test
    void update_mapsPartialItemFields() throws Exception {
        mockMvc.perform(patch("/api/v1/items/10")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Milk",
                                  "expiryDate": null,
                                  "memo": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));

        ArgumentCaptor<UpdateIngredientUseCase.Command> captor =
                ArgumentCaptor.forClass(UpdateIngredientUseCase.Command.class);
        verify(updateIngredientUseCase).update(captor.capture());
        UpdateIngredientUseCase.Command command = captor.getValue();
        assertAll(
                () -> assertEquals(10L, command.ingredientId()),
                () -> assertEquals(1L, command.userId()),
                () -> assertNull(command.storageType()),
                () -> assertEquals("Milk", command.name()),
                () -> assertTrue(command.expiresAtSet()),
                () -> assertNull(command.expiresAt()),
                () -> assertTrue(command.noteSet()),
                () -> assertNull(command.note()),
                () -> assertNull(command.expirySourceType()),
                () -> assertNull(command.sourceType())
        );
    }

    @Test
    void update_mapsStorageType() throws Exception {
        mockMvc.perform(patch("/api/v1/items/10")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storageType": "FREEZER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));

        ArgumentCaptor<UpdateIngredientUseCase.Command> captor =
                ArgumentCaptor.forClass(UpdateIngredientUseCase.Command.class);
        verify(updateIngredientUseCase).update(captor.capture());
        UpdateIngredientUseCase.Command command = captor.getValue();
        assertAll(
                () -> assertEquals(10L, command.ingredientId()),
                () -> assertEquals(1L, command.userId()),
                () -> assertEquals(StorageType.FREEZER, command.storageType())
        );
    }

    @Test
    void update_returnsNotFoundForDiscardedItem() throws Exception {
        org.mockito.Mockito.doThrow(new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND))
                .when(updateIngredientUseCase)
                .update(any(UpdateIngredientUseCase.Command.class));

        mockMvc.perform(patch("/api/v1/items/10")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Milk"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/v1/items/10"));
    }

    @Test
    void delete_discardsItem() throws Exception {
        mockMvc.perform(delete("/api/v1/items/10")
                        .principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));

        verify(deleteIngredientUseCase).delete(new DeleteIngredientUseCase.Command(10L, 1L));
    }

    @Test
    void storages_returnsUserStorages() throws Exception {
        when(listStoragesUseCase.list(any(ListStoragesUseCase.Query.class))).thenReturn(List.of(
                new IngredientDto.StorageSummaryResponse(2L, StorageType.FRIDGE, "Fridge")
        ));

        mockMvc.perform(get("/api/v1/items/storages")
                        .principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].storageId").value(2))
                .andExpect(jsonPath("$.data[0].storageType").value("FRIDGE"))
                .andExpect(jsonPath("$.data[0].name").value("Fridge"));
    }

    @Test
    void create_withBlankName_returnsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/items")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "storageType": "FRIDGE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value("/api/v1/items"));
    }

    @Test
    void update_withLegacyStorageId_returnsInvalidInput() throws Exception {
        mockMvc.perform(patch("/api/v1/items/10")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storageId": 2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value("/api/v1/items/10"));
    }

    @Test
    void update_withBlankName_returnsInvalidInput() throws Exception {
        mockMvc.perform(patch("/api/v1/items/10")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value("/api/v1/items/10"));
    }

    private static IngredientDto.SummaryResponse summary() {
        return new IngredientDto.SummaryResponse(
                10L,
                "Tomato",
                IngredientStatus.ACTIVE,
                2L,
                "Fridge",
                StorageType.FRIDGE,
                3L,
                CatalogCategory.VEGETABLE,
                "🍅",
                LocalDate.of(2026, 5, 6),
                LocalDate.of(2026, 4, 29),
                "salad",
                null,
                new IngredientDto.RepresentativeImage(
                        IngredientDto.RepresentativeImageType.EMOJI, null, null, "🍅",
                        IngredientDto.RepresentativeImageSource.CATALOG_EMOJI)
        );
    }

    private static IngredientDto.DetailResponse detail() {
        return new IngredientDto.DetailResponse(
                10L,
                1L,
                2L,
                "Fridge",
                StorageType.FRIDGE,
                3L,
                "Tomato",
                CatalogCategory.VEGETABLE,
                "🍅",
                "Tomato",
                LocalDate.of(2026, 4, 29),
                LocalDate.of(2026, 5, 6),
                ExpirySourceType.MANUAL,
                IngredientStatus.ACTIVE,
                null,
                null,
                "salad",
                IngredientSourceType.MANUAL,
                null,
                new IngredientDto.RepresentativeImage(
                        IngredientDto.RepresentativeImageType.EMOJI, null, null, "🍅",
                        IngredientDto.RepresentativeImageSource.CATALOG_EMOJI)
        );
    }

    private static TestingAuthenticationToken auth(Long userId) {
        return new TestingAuthenticationToken(userId, null);
    }

    private static class TestAuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(Long.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory
        ) {
            TestingAuthenticationToken authentication =
                    (TestingAuthenticationToken) webRequest.getUserPrincipal();
            return authentication != null ? authentication.getPrincipal() : null;
        }
    }
}
