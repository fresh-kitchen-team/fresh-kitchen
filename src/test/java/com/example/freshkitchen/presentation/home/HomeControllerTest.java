package com.example.freshkitchen.presentation.home;

import com.example.freshkitchen.application.home.dto.HomeDto;
import com.example.freshkitchen.application.home.dto.HomeDto.HomeIngredientStatus;
import com.example.freshkitchen.application.home.usecase.GetHomeSummaryUseCase;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.global.exception.handler.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HomeControllerTest {

    private GetHomeSummaryUseCase getHomeSummaryUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        getHomeSummaryUseCase = mock(GetHomeSummaryUseCase.class);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new HomeController(getHomeSummaryUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void summary_returnsHomeSummary() throws Exception {
        when(getHomeSummaryUseCase.get(any(GetHomeSummaryUseCase.Query.class))).thenReturn(
                new HomeDto.SummaryResponse(
                        42,
                        35,
                        5,
                        2,
                        List.of(new HomeDto.StorageSummaryResponse(StorageType.FRIDGE, "🥛", "냉장실", 28, "fridge")),
                        List.of(new HomeDto.ItemPreviewResponse(
                                4L,
                                "계란",
                                StorageType.FRIDGE,
                                LocalDate.of(2026, 3, 24),
                                HomeIngredientStatus.NEAR_EXPIRY,
                                "🥚"
                        )),
                        List.of(),
                        List.of()
                )
        );

        mockMvc.perform(get("/api/v1/home/summary")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.totalCount").value(42))
                .andExpect(jsonPath("$.data.freshCount").value(35))
                .andExpect(jsonPath("$.data.nearExpiryCount").value(5))
                .andExpect(jsonPath("$.data.expiredCount").value(2))
                .andExpect(jsonPath("$.data.storages[0].storage").value("FRIDGE"))
                .andExpect(jsonPath("$.data.storages[0].emoji").value("🥛"))
                .andExpect(jsonPath("$.data.storages[0].name").value("냉장실"))
                .andExpect(jsonPath("$.data.storages[0].itemCount").value(28))
                .andExpect(jsonPath("$.data.storages[0].filterKey").value("fridge"))
                .andExpect(jsonPath("$.data.nearExpiryItems[0].id").value(4))
                .andExpect(jsonPath("$.data.nearExpiryItems[0].name").value("계란"))
                .andExpect(jsonPath("$.data.nearExpiryItems[0].storage").value("FRIDGE"))
                .andExpect(jsonPath("$.data.nearExpiryItems[0].expiryDate").value("2026-03-24"))
                .andExpect(jsonPath("$.data.nearExpiryItems[0].status").value("NEAR_EXPIRY"))
                .andExpect(jsonPath("$.data.nearExpiryItems[0].emoji").value("🥚"));

        ArgumentCaptor<GetHomeSummaryUseCase.Query> captor = ArgumentCaptor.forClass(GetHomeSummaryUseCase.Query.class);
        verify(getHomeSummaryUseCase).get(captor.capture());
        assertEquals(1L, captor.getValue().userId());
    }

    @Test
    void summary_withoutUserIdHeader_returnsInvalidInput() throws Exception {
        mockMvc.perform(get("/api/v1/home/summary"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.path").value("/api/v1/home/summary"));
    }

    @Test
    void summary_withInvalidUserIdHeader_returnsInvalidInput() throws Exception {
        mockMvc.perform(get("/api/v1/home/summary")
                        .header("X-User-Id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.path").value("/api/v1/home/summary"));
    }

    @Test
    void summary_withNonPositiveUserId_returnsInvalidInput() throws Exception {
        mockMvc.perform(get("/api/v1/home/summary")
                        .header("X-User-Id", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.path").value("/api/v1/home/summary"));
    }
}
