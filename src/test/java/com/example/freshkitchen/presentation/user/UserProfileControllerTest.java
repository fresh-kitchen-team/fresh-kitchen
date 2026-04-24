package com.example.freshkitchen.presentation.user;

import com.example.freshkitchen.application.user.dto.UserProfileResult;
import com.example.freshkitchen.application.user.usecase.GetUserProfileUseCase;
import com.example.freshkitchen.domain.user.enums.AllergyType;
import com.example.freshkitchen.domain.user.enums.CookingTool;
import com.example.freshkitchen.domain.user.enums.FoodStyle;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.global.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileControllerTest {

    private final GetUserProfileUseCase getUserProfileUseCase = mock(GetUserProfileUseCase.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new UserProfileController(getUserProfileUseCase))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void getProfile_returnsProfile() throws Exception {
        GetUserProfileUseCase.Query query = new GetUserProfileUseCase.Query(1L);
        given(getUserProfileUseCase.get(query))
                .willReturn(new UserProfileResult(
                        1L,
                        "fresh-chef",
                        "https://example.com/profile.png",
                        "fridge clean-up mode",
                        Set.of("Tomato"),
                        Set.of(FoodStyle.KOREAN),
                        Set.of(AllergyType.EGG),
                        Set.of(CookingTool.AIR_FRYER)
                ));

        mockMvc.perform(get("/api/v1/users/{userId}/profile", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.nickname").value("fresh-chef"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://example.com/profile.png"))
                .andExpect(jsonPath("$.bio").value("fridge clean-up mode"))
                .andExpect(jsonPath("$.preferredIngredients", containsInAnyOrder("Tomato")))
                .andExpect(jsonPath("$.foodStyles", containsInAnyOrder("KOREAN")))
                .andExpect(jsonPath("$.allergies", containsInAnyOrder("EGG")))
                .andExpect(jsonPath("$.cookingTools", containsInAnyOrder("AIR_FRYER")));

        then(getUserProfileUseCase).should().get(query);
    }

    @Test
    void getProfile_returnsEmptyProfileWhenUserHasNoProfile() throws Exception {
        GetUserProfileUseCase.Query query = new GetUserProfileUseCase.Query(2L);
        given(getUserProfileUseCase.get(query))
                .willReturn(new UserProfileResult(
                        2L,
                        null,
                        null,
                        null,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of()
                ));

        mockMvc.perform(get("/api/v1/users/{userId}/profile", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.nickname").doesNotExist())
                .andExpect(jsonPath("$.profileImageUrl").doesNotExist())
                .andExpect(jsonPath("$.bio").doesNotExist())
                .andExpect(jsonPath("$.preferredIngredients.length()").value(0))
                .andExpect(jsonPath("$.foodStyles.length()").value(0))
                .andExpect(jsonPath("$.allergies.length()").value(0))
                .andExpect(jsonPath("$.cookingTools.length()").value(0));

        then(getUserProfileUseCase).should().get(query);
    }

    @Test
    void getProfile_returnsNotFoundWhenUserDoesNotExist() throws Exception {
        GetUserProfileUseCase.Query query = new GetUserProfileUseCase.Query(99L);
        given(getUserProfileUseCase.get(query))
                .willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/users/{userId}/profile", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("USER-404-1"))
                .andExpect(jsonPath("$.message").value("user not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/99/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        then(getUserProfileUseCase).should().get(query);
    }
}
