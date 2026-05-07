package com.example.freshkitchen.presentation.ingredient;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.GetIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ListIngredientsUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ListStoragesUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ResolveIngredientDefaultsUseCase;
import com.example.freshkitchen.application.ingredient.usecase.UpdateIngredientUseCase;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IngredientControllerTest {

    private CreateIngredientUseCase createIngredientUseCase;
    private UpdateIngredientUseCase updateIngredientUseCase;
    private GetIngredientUseCase getIngredientUseCase;
    private ListIngredientsUseCase listIngredientsUseCase;
    private ResolveIngredientDefaultsUseCase resolveIngredientDefaultsUseCase;
    private ListStoragesUseCase listStoragesUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createIngredientUseCase = mock(CreateIngredientUseCase.class);
        updateIngredientUseCase = mock(UpdateIngredientUseCase.class);
        getIngredientUseCase = mock(GetIngredientUseCase.class);
        listIngredientsUseCase = mock(ListIngredientsUseCase.class);
        resolveIngredientDefaultsUseCase = mock(ResolveIngredientDefaultsUseCase.class);
        listStoragesUseCase = mock(ListStoragesUseCase.class);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        IngredientController controller = new IngredientController(
                createIngredientUseCase,
                updateIngredientUseCase,
                getIngredientUseCase,
                listIngredientsUseCase,
                resolveIngredientDefaultsUseCase,
                listStoragesUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void create_returnsCreatedIngredientId() throws Exception {
        when(createIngredientUseCase.create(any(CreateIngredientUseCase.Command.class))).thenReturn(10L);

        mockMvc.perform(post("/api/v1/ingredients")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storageId": 2,
                                  "catalogId": 3,
                                  "name": "Tomato",
                                  "registeredAt": "2026-04-29",
                                  "expiresAt": "2026-05-06",
                                  "expirySourceType": "POLICY",
                                  "note": "salad",
                                  "sourceType": "MANUAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingredientId").value(10));

        ArgumentCaptor<CreateIngredientUseCase.Command> captor = ArgumentCaptor.forClass(CreateIngredientUseCase.Command.class);
        verify(createIngredientUseCase).create(captor.capture());
        CreateIngredientUseCase.Command command = captor.getValue();
        assertAll(
                () -> assertEquals(1L, command.userId()),
                () -> assertEquals(2L, command.storageId()),
                () -> assertEquals(3L, command.catalogId()),
                () -> assertEquals("Tomato", command.name()),
                () -> assertEquals(LocalDate.of(2026, 4, 29), command.registeredAt()),
                () -> assertEquals(LocalDate.of(2026, 5, 6), command.expiresAt()),
                () -> assertEquals(ExpirySourceType.POLICY, command.expirySourceType()),
                () -> assertEquals("salad", command.note()),
                () -> assertEquals(IngredientSourceType.MANUAL, command.sourceType())
        );
    }

    @Test
    void update_mapsExplicitNullFieldsAsSet() throws Exception {
        mockMvc.perform(patch("/api/v1/ingredients/10")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogId": null,
                                  "registeredAt": null,
                                  "note": null,
                                  "name": "Milk"
                                }
                                """))
                .andExpect(status().isNoContent());

        ArgumentCaptor<UpdateIngredientUseCase.Command> captor = ArgumentCaptor.forClass(UpdateIngredientUseCase.Command.class);
        verify(updateIngredientUseCase).update(captor.capture());
        UpdateIngredientUseCase.Command command = captor.getValue();
        assertAll(
                () -> assertEquals(10L, command.ingredientId()),
                () -> assertEquals(1L, command.userId()),
                () -> assertTrue(command.catalogSet()),
                () -> assertNull(command.catalogId()),
                () -> assertEquals("Milk", command.name()),
                () -> assertTrue(command.registeredAtSet()),
                () -> assertNull(command.registeredAt()),
                () -> assertTrue(command.noteSet()),
                () -> assertNull(command.note())
        );
    }

    @Test
    void get_returnsIngredientDetail() throws Exception {
        when(getIngredientUseCase.get(any(GetIngredientUseCase.Query.class))).thenReturn(new IngredientDto.DetailResponse(
                10L,
                1L,
                2L,
                "Fridge",
                StorageType.FRIDGE,
                3L,
                "Tomato",
                CatalogCategory.VEGETABLE,
                "Tomato",
                LocalDate.of(2026, 4, 29),
                LocalDate.of(2026, 5, 6),
                ExpirySourceType.POLICY,
                IngredientStatus.ACTIVE,
                null,
                null,
                "salad",
                IngredientSourceType.MANUAL
        ));

        mockMvc.perform(get("/api/v1/ingredients/10")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredientId").value(10))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.name").value("Tomato"))
                .andExpect(jsonPath("$.storageType").value("FRIDGE"));

        ArgumentCaptor<GetIngredientUseCase.Query> captor = ArgumentCaptor.forClass(GetIngredientUseCase.Query.class);
        verify(getIngredientUseCase).get(captor.capture());
        assertAll(
                () -> assertEquals(10L, captor.getValue().ingredientId()),
                () -> assertEquals(1L, captor.getValue().userId())
        );
    }

    @Test
    void list_returnsIngredientSummaries() throws Exception {
        when(listIngredientsUseCase.list(any(ListIngredientsUseCase.Query.class))).thenReturn(List.of(
                new IngredientDto.SummaryResponse(
                        10L,
                        "Tomato",
                        IngredientStatus.ACTIVE,
                        2L,
                        "Fridge",
                        StorageType.FRIDGE,
                        3L,
                        LocalDate.of(2026, 5, 6)
                )
        ));

        mockMvc.perform(get("/api/v1/ingredients")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ingredientId").value(10))
                .andExpect(jsonPath("$[0].name").value("Tomato"));

        ArgumentCaptor<ListIngredientsUseCase.Query> captor = ArgumentCaptor.forClass(ListIngredientsUseCase.Query.class);
        verify(listIngredientsUseCase).list(captor.capture());
        assertEquals(1L, captor.getValue().userId());
    }

    @Test
    void defaults_returnsResolvedDefaults() throws Exception {
        when(resolveIngredientDefaultsUseCase.resolve(any(ResolveIngredientDefaultsUseCase.Query.class))).thenReturn(
                new IngredientDto.DefaultsResponse(3L, StorageType.FRIDGE, 7, "default rule")
        );

        mockMvc.perform(get("/api/v1/ingredients/defaults")
                        .queryParam("catalogId", "3")
                        .queryParam("category", "VEGETABLE")
                        .queryParam("storageType", "FRIDGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.catalogId").value(3))
                .andExpect(jsonPath("$.defaultStorageType").value("FRIDGE"))
                .andExpect(jsonPath("$.shelfLifeDays").value(7));

        ArgumentCaptor<ResolveIngredientDefaultsUseCase.Query> captor = ArgumentCaptor.forClass(ResolveIngredientDefaultsUseCase.Query.class);
        verify(resolveIngredientDefaultsUseCase).resolve(captor.capture());
        assertAll(
                () -> assertEquals(3L, captor.getValue().catalogId()),
                () -> assertEquals(CatalogCategory.VEGETABLE, captor.getValue().category()),
                () -> assertEquals(StorageType.FRIDGE, captor.getValue().storageType())
        );
    }

    @Test
    void storages_returnsUserStorages() throws Exception {
        when(listStoragesUseCase.list(any(ListStoragesUseCase.Query.class))).thenReturn(List.of(
                new IngredientDto.StorageSummaryResponse(2L, StorageType.FRIDGE, "Fridge")
        ));

        mockMvc.perform(get("/api/v1/ingredients/storages")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storageId").value(2))
                .andExpect(jsonPath("$[0].storageType").value("FRIDGE"));

        ArgumentCaptor<ListStoragesUseCase.Query> captor = ArgumentCaptor.forClass(ListStoragesUseCase.Query.class);
        verify(listStoragesUseCase).list(captor.capture());
        assertEquals(1L, captor.getValue().userId());
    }

    @Test
    void create_withoutUserIdHeader_returnsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storageId": 2,
                                  "name": "Tomato",
                                  "expirySourceType": "POLICY",
                                  "sourceType": "MANUAL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.path").value("/api/v1/ingredients"));
    }

    @Test
    void create_withInvalidUserIdHeader_returnsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/ingredients")
                        .header("X-User-Id", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storageId": 2,
                                  "name": "Tomato",
                                  "expirySourceType": "POLICY",
                                  "sourceType": "MANUAL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.path").value("/api/v1/ingredients"));
    }

    @Test
    void create_withBlankName_returnsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/ingredients")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storageId": 2,
                                  "name": "",
                                  "expirySourceType": "POLICY",
                                  "sourceType": "MANUAL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.path").value("/api/v1/ingredients"));
    }

    @Test
    void update_withNonObjectBody_returnsInvalidInput() throws Exception {
        mockMvc.perform(patch("/api/v1/ingredients/10")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.path").value("/api/v1/ingredients/10"));
    }

    @Test
    void update_withInvalidDate_returnsInvalidInput() throws Exception {
        mockMvc.perform(patch("/api/v1/ingredients/10")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "registeredAt": "not-a-date"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.path").value("/api/v1/ingredients/10"));
    }

    @Test
    void update_withInvalidEnum_returnsInvalidInput() throws Exception {
        mockMvc.perform(patch("/api/v1/ingredients/10")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceType": "UNKNOWN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.path").value("/api/v1/ingredients/10"));
    }

    @Test
    void update_withInvalidFieldType_returnsInvalidInput() throws Exception {
        mockMvc.perform(patch("/api/v1/ingredients/10")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storageId": "2"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.path").value("/api/v1/ingredients/10"));
    }
}
