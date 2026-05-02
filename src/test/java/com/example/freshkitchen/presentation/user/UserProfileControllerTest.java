package com.example.freshkitchen.presentation.user;

import com.example.freshkitchen.application.user.dto.UserProfileResult;
import com.example.freshkitchen.application.user.usecase.DeleteUserProfileUseCase;
import com.example.freshkitchen.application.user.usecase.GetUserProfileUseCase;
import com.example.freshkitchen.application.user.usecase.UpdateUserProfileUseCase;
import com.example.freshkitchen.domain.user.enums.AllergyType;
import com.example.freshkitchen.domain.user.enums.CookingTool;
import com.example.freshkitchen.domain.user.enums.FoodStyle;
import com.example.freshkitchen.domain.user.exception.UserErrorCode;
import com.example.freshkitchen.domain.user.exception.UserException;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.global.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileControllerTest {

    private final GetUserProfileUseCase getUserProfileUseCase = mock(GetUserProfileUseCase.class);
    private final UpdateUserProfileUseCase updateUserProfileUseCase = mock(UpdateUserProfileUseCase.class);
    private final DeleteUserProfileUseCase deleteUserProfileUseCase = mock(DeleteUserProfileUseCase.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new UserProfileController(
                    getUserProfileUseCase,
                    updateUserProfileUseCase,
                    deleteUserProfileUseCase
            ))
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
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.nickname").value("fresh-chef"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/profile.png"))
                .andExpect(jsonPath("$.data.bio").value("fridge clean-up mode"))
                .andExpect(jsonPath("$.data.preferredIngredients", containsInAnyOrder("Tomato")))
                .andExpect(jsonPath("$.data.foodStyles", containsInAnyOrder("KOREAN")))
                .andExpect(jsonPath("$.data.allergies", containsInAnyOrder("EGG")))
                .andExpect(jsonPath("$.data.cookingTools", containsInAnyOrder("AIR_FRYER")));

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
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.userId").value(2))
                .andExpect(jsonPath("$.data.nickname").value(nullValue()))
                .andExpect(jsonPath("$.data.profileImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.bio").value(nullValue()))
                .andExpect(jsonPath("$.data.preferredIngredients.length()").value(0))
                .andExpect(jsonPath("$.data.foodStyles.length()").value(0))
                .andExpect(jsonPath("$.data.allergies.length()").value(0))
                .andExpect(jsonPath("$.data.cookingTools.length()").value(0));

        then(getUserProfileUseCase).should().get(query);
    }

    @Test
    void getProfile_returnsBadRequestWhenUserIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/profile", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/abc/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(getUserProfileUseCase);
    }

    @Test
    void updateProfile_updatesProfile() throws Exception {
        UpdateUserProfileUseCase.Command command = new UpdateUserProfileUseCase.Command(
                1L,
                "fresh-chef",
                "https://example.com/profile.png",
                true,
                null,
                true,
                Set.of("Tomato", "Egg"),
                Set.of(FoodStyle.KOREAN),
                Set.of(AllergyType.EGG),
                Set.of(CookingTool.AIR_FRYER)
        );

        mockMvc.perform(patch("/api/v1/users/{userId}/profile", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "fresh-chef",
                                  "profileImageUrl": "https://example.com/profile.png",
                                  "bio": null,
                                  "preferredIngredients": ["Tomato", "Egg"],
                                  "foodStyles": ["KOREAN"],
                                  "allergies": ["EGG"],
                                  "cookingTools": ["AIR_FRYER"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        then(updateUserProfileUseCase).should().update(command);
    }

    @Test
    void updateProfile_keepsUnsetNullableFields() throws Exception {
        UpdateUserProfileUseCase.Command command = new UpdateUserProfileUseCase.Command(
                1L,
                "fresh-chef",
                null,
                false,
                null,
                false,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(patch("/api/v1/users/{userId}/profile", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "fresh-chef"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));

        then(updateUserProfileUseCase).should().update(command);
    }

    @Test
    void updateProfile_clearsNullableFieldsWhenExplicitNullIsSent() throws Exception {
        UpdateUserProfileUseCase.Command command = new UpdateUserProfileUseCase.Command(
                1L,
                null,
                null,
                true,
                null,
                true,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(patch("/api/v1/users/{userId}/profile", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileImageUrl": null,
                                  "bio": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));

        then(updateUserProfileUseCase).should().update(command);
    }

    @Test
    void updateProfile_returnsBadRequestWhenUseCaseRejectsInitialCreation() throws Exception {
        UpdateUserProfileUseCase.Command command = new UpdateUserProfileUseCase.Command(
                1L,
                null,
                null,
                false,
                null,
                false,
                null,
                null,
                null,
                null
        );
        willThrow(new BusinessValidationException("nickname must not be blank"))
                .given(updateUserProfileUseCase)
                .update(command);

        mockMvc.perform(patch("/api/v1/users/{userId}/profile", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/1/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        then(updateUserProfileUseCase).should().update(command);
    }

    @Test
    void updateProfile_returnsNotFoundWhenUserDoesNotExist() throws Exception {
        UpdateUserProfileUseCase.Command command = new UpdateUserProfileUseCase.Command(
                99L,
                "fresh-chef",
                null,
                false,
                null,
                false,
                null,
                null,
                null,
                null
        );
        willThrow(new UserException(UserErrorCode.USER_NOT_FOUND))
                .given(updateUserProfileUseCase)
                .update(command);

        mockMvc.perform(patch("/api/v1/users/{userId}/profile", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "fresh-chef"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("USER-404-1"))
                .andExpect(jsonPath("$.message").value("user not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/99/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        then(updateUserProfileUseCase).should().update(command);
    }

    @Test
    void updateProfile_returnsBadRequestWhenEnumValueIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{userId}/profile", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "foodStyles": ["UNKNOWN"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/1/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(updateUserProfileUseCase);
    }

    @Test
    void updateProfile_returnsBadRequestWhenJsonIsMalformed() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{userId}/profile", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/1/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(updateUserProfileUseCase);
    }

    @Test
    void deleteProfile_deletesProfile() throws Exception {
        DeleteUserProfileUseCase.Command command = new DeleteUserProfileUseCase.Command(1L);

        mockMvc.perform(delete("/api/v1/users/{userId}/profile", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        then(deleteUserProfileUseCase).should().delete(command);
    }

    @Test
    void deleteProfile_returnsNotFoundWhenUserDoesNotExist() throws Exception {
        DeleteUserProfileUseCase.Command command = new DeleteUserProfileUseCase.Command(99L);
        willThrow(new UserException(UserErrorCode.USER_NOT_FOUND))
                .given(deleteUserProfileUseCase)
                .delete(command);

        mockMvc.perform(delete("/api/v1/users/{userId}/profile", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("USER-404-1"))
                .andExpect(jsonPath("$.message").value("user not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/99/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        then(deleteUserProfileUseCase).should().delete(command);
    }

    @Test
    void deleteProfile_returnsBadRequestWhenUserIdIsNotNumeric() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{userId}/profile", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/abc/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(deleteUserProfileUseCase);
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
