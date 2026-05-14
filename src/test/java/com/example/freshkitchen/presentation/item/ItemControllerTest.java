package com.example.freshkitchen.presentation.item;

import com.example.freshkitchen.application.image.usecase.AttachIngredientImageUseCase;
import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.DeleteIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.GetIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ListIngredientsUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ListStoragesUseCase;
import com.example.freshkitchen.application.ingredient.usecase.UpdateIngredientUseCase;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
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

    private CreateIngredientUseCase createIngredientUseCase;
    private UpdateIngredientUseCase updateIngredientUseCase;
    private GetIngredientUseCase getIngredientUseCase;
    private ListIngredientsUseCase listIngredientsUseCase;
    private DeleteIngredientUseCase deleteIngredientUseCase;
    private ListStoragesUseCase listStoragesUseCase;
    private AttachIngredientImageUseCase attachIngredientImageUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createIngredientUseCase = mock(CreateIngredientUseCase.class);
        updateIngredientUseCase = mock(UpdateIngredientUseCase.class);
        getIngredientUseCase = mock(GetIngredientUseCase.class);
        listIngredientsUseCase = mock(ListIngredientsUseCase.class);
        deleteIngredientUseCase = mock(DeleteIngredientUseCase.class);
        listStoragesUseCase = mock(ListStoragesUseCase.class);
        attachIngredientImageUseCase = mock(AttachIngredientImageUseCase.class);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ItemController controller = new ItemController(
                createIngredientUseCase,
                updateIngredientUseCase,
                getIngredientUseCase,
                listIngredientsUseCase,
                deleteIngredientUseCase,
                listStoragesUseCase,
                attachIngredientImageUseCase,
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
        when(createIngredientUseCase.create(any(CreateIngredientUseCase.Command.class))).thenReturn(10L);

        mockMvc.perform(post("/api/v1/items")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tomato",
                                  "catalogId": 3,
                                  "storageId": 2,
                                  "expiryDate": "2026-05-06",
                                  "purchaseDate": "2026-04-29",
                                  "memo": "salad",
                                  "imageAssetId": 20
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value(10));

        ArgumentCaptor<CreateIngredientUseCase.Command> createCaptor =
                ArgumentCaptor.forClass(CreateIngredientUseCase.Command.class);
        verify(createIngredientUseCase).create(createCaptor.capture());
        assertAll(
                () -> assertEquals(1L, createCaptor.getValue().userId()),
                () -> assertEquals(2L, createCaptor.getValue().storageId()),
                () -> assertEquals(3L, createCaptor.getValue().catalogId()),
                () -> assertEquals("Tomato", createCaptor.getValue().name()),
                () -> assertEquals(LocalDate.of(2026, 4, 29), createCaptor.getValue().registeredAt()),
                () -> assertEquals(LocalDate.of(2026, 5, 6), createCaptor.getValue().expiresAt()),
                () -> assertEquals(ExpirySourceType.MANUAL, createCaptor.getValue().expirySourceType()),
                () -> assertEquals("salad", createCaptor.getValue().note()),
                () -> assertEquals(IngredientSourceType.PHOTO, createCaptor.getValue().sourceType())
        );

        verify(attachIngredientImageUseCase).attach(new AttachIngredientImageUseCase.Command(
                1L,
                10L,
                20L,
                true,
                IngredientImageSourceType.PHOTO
        ));
    }

    @Test
    void create_defaultsPurchaseDateToToday() throws Exception {
        when(createIngredientUseCase.create(any(CreateIngredientUseCase.Command.class))).thenReturn(10L);

        mockMvc.perform(post("/api/v1/items")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tomato",
                                  "catalogId": 3,
                                  "storageId": 2
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateIngredientUseCase.Command> captor =
                ArgumentCaptor.forClass(CreateIngredientUseCase.Command.class);
        verify(createIngredientUseCase).create(captor.capture());
        assertEquals(LocalDate.of(2026, 5, 1), captor.getValue().registeredAt());
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
                                  "catalogId": 3,
                                  "storageId": 2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value("/api/v1/items"));
    }

    @Test
    void update_withExplicitNullForRequiredPatchField_returnsInvalidInput() throws Exception {
        mockMvc.perform(patch("/api/v1/items/10")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storageId": null
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
                null
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
                null
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
