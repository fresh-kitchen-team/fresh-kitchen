package com.example.freshkitchen.presentation.home;

import com.example.freshkitchen.application.home.dto.HomeDto;
import com.example.freshkitchen.application.home.dto.HomeDto.HomeIngredientStatus;
import com.example.freshkitchen.application.home.usecase.GetHomeSummaryUseCase;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerTest {

    @MockitoBean
    private GetHomeSummaryUseCase getHomeSummaryUseCase;

    @Autowired
    private MockMvc mockMvc;

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

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(1L, null));
        try {
            mockMvc.perform(get("/api/v1/home/summary"))
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
        } finally {
            SecurityContextHolder.clearContext();
        }

        ArgumentCaptor<GetHomeSummaryUseCase.Query> queryCaptor =
                ArgumentCaptor.forClass(GetHomeSummaryUseCase.Query.class);
        verify(getHomeSummaryUseCase).get(queryCaptor.capture());
        assertEquals(1L, queryCaptor.getValue().userId());
    }

    @Test
    void summary_withoutUserIdHeader_returnsInvalidInput() throws Exception {
        when(getHomeSummaryUseCase.get(any(GetHomeSummaryUseCase.Query.class)))
                .thenThrow(new BusinessValidationException("userId must not be null"));

        mockMvc.perform(get("/api/v1/home/summary"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.path").value("/api/v1/home/summary"));
    }

    @Test
    void summary_withInvalidUserIdHeader_returnsInvalidInput() throws Exception {
        when(getHomeSummaryUseCase.get(any(GetHomeSummaryUseCase.Query.class)))
                .thenThrow(new BusinessValidationException("userId must not be null"));

        mockMvc.perform(get("/api/v1/home/summary")
                        .header("X-User-Id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.path").value("/api/v1/home/summary"));
    }

    @Test
    void summary_withNonPositiveUserId_returnsInvalidInput() throws Exception {
        when(getHomeSummaryUseCase.get(any(GetHomeSummaryUseCase.Query.class)))
                .thenThrow(new BusinessValidationException("userId must be positive"));

        mockMvc.perform(get("/api/v1/home/summary")
                        .with(authentication(new TestingAuthenticationToken(0L, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.path").value("/api/v1/home/summary"));
    }
}
