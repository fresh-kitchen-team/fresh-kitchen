package com.example.freshkitchen.presentation.user;

import com.example.freshkitchen.application.user.dto.UserProfileResult;
import com.example.freshkitchen.application.user.usecase.DeleteUserProfileUseCase;
import com.example.freshkitchen.application.user.usecase.DeleteUserUseCase;
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
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

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

    private static final Long TEST_USER_ID = 1L;

    private final GetUserProfileUseCase getUserProfileUseCase = mock(GetUserProfileUseCase.class);
    private final UpdateUserProfileUseCase updateUserProfileUseCase = mock(UpdateUserProfileUseCase.class);
    private final DeleteUserProfileUseCase deleteUserProfileUseCase = mock(DeleteUserProfileUseCase.class);
    private final DeleteUserUseCase deleteUserUseCase = mock(DeleteUserUseCase.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new UserProfileController(
                    getUserProfileUseCase,
                    updateUserProfileUseCase,
                    deleteUserProfileUseCase,
                    deleteUserUseCase
            ))
            .setCustomArgumentResolvers(authenticationPrincipalResolver(TEST_USER_ID))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void getProfile_returnsProfile() throws Exception {
        GetUserProfileUseCase.Query query = new GetUserProfileUseCase.Query(TEST_USER_ID);
        given(getUserProfileUseCase.get(query))
                .willReturn(new UserProfileResult(
                        TEST_USER_ID,
                        "fresh-chef",
                        "https://example.com/profile.png",
                        "fridge clean-up mode",
                        Set.of("Tomato"),
                        Set.of(FoodStyle.KOREAN),
                        Set.of(AllergyType.EGG),
                        Set.of(CookingTool.AIR_FRYER)
                ));

        mockMvc.perform(get("/api/v1/users/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID.intValue()))
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
        GetUserProfileUseCase.Query query = new GetUserProfileUseCase.Query(TEST_USER_ID);
        given(getUserProfileUseCase.get(query))
                .willReturn(new UserProfileResult(
                        TEST_USER_ID,
                        null,
                        null,
                        null,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        Set.of()
                ));

        mockMvc.perform(get("/api/v1/users/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID.intValue()))
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
    void updateProfile_updatesProfile() throws Exception {
        UpdateUserProfileUseCase.Command command = new UpdateUserProfileUseCase.Command(
                TEST_USER_ID,
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

        mockMvc.perform(patch("/api/v1/users/me/profile")
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
                TEST_USER_ID,
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

        mockMvc.perform(patch("/api/v1/users/me/profile")
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
                TEST_USER_ID,
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

        mockMvc.perform(patch("/api/v1/users/me/profile")
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
                TEST_USER_ID,
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

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        then(updateUserProfileUseCase).should().update(command);
    }

    @Test
    void updateProfile_returnsNotFoundWhenUserDoesNotExist() throws Exception {
        MockMvc notFoundMockMvc = MockMvcBuilders
                .standaloneSetup(new UserProfileController(
                        getUserProfileUseCase,
                        updateUserProfileUseCase,
                        deleteUserProfileUseCase,
                        deleteUserUseCase
                ))
                .setCustomArgumentResolvers(authenticationPrincipalResolver(99L))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

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

        notFoundMockMvc.perform(patch("/api/v1/users/me/profile")
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
                .andExpect(jsonPath("$.path").value("/api/v1/users/me/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        then(updateUserProfileUseCase).should().update(command);
    }

    @Test
    void updateProfile_returnsBadRequestWhenEnumValueIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/profile")
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
                .andExpect(jsonPath("$.path").value("/api/v1/users/me/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(updateUserProfileUseCase);
    }

    @Test
    void updateProfile_returnsBadRequestWhenJsonIsMalformed() throws Exception {
        String malformedJson = "{";
        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(updateUserProfileUseCase);
    }

    @Test
    void deleteProfile_deletesProfile() throws Exception {
        DeleteUserProfileUseCase.Command command = new DeleteUserProfileUseCase.Command(TEST_USER_ID);

        mockMvc.perform(delete("/api/v1/users/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        then(deleteUserProfileUseCase).should().delete(command);
    }

    @Test
    void deleteProfile_returnsNotFoundWhenUserDoesNotExist() throws Exception {
        MockMvc notFoundMockMvc = MockMvcBuilders
                .standaloneSetup(new UserProfileController(
                        getUserProfileUseCase,
                        updateUserProfileUseCase,
                        deleteUserProfileUseCase,
                        deleteUserUseCase
                ))
                .setCustomArgumentResolvers(authenticationPrincipalResolver(99L))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        DeleteUserProfileUseCase.Command command = new DeleteUserProfileUseCase.Command(99L);
        willThrow(new UserException(UserErrorCode.USER_NOT_FOUND))
                .given(deleteUserProfileUseCase)
                .delete(command);

        notFoundMockMvc.perform(delete("/api/v1/users/me/profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("USER-404-1"))
                .andExpect(jsonPath("$.message").value("user not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        then(deleteUserProfileUseCase).should().delete(command);
    }

    @Test
    void getProfile_returnsNotFoundWhenUserDoesNotExist() throws Exception {
        MockMvc notFoundMockMvc = MockMvcBuilders
                .standaloneSetup(new UserProfileController(
                        getUserProfileUseCase,
                        updateUserProfileUseCase,
                        deleteUserProfileUseCase,
                        deleteUserUseCase
                ))
                .setCustomArgumentResolvers(authenticationPrincipalResolver(99L))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        GetUserProfileUseCase.Query query = new GetUserProfileUseCase.Query(99L);
        given(getUserProfileUseCase.get(query))
                .willThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        notFoundMockMvc.perform(get("/api/v1/users/me/profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("USER-404-1"))
                .andExpect(jsonPath("$.message").value("user not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me/profile"))
                .andExpect(jsonPath("$.timestamp").exists());

        then(getUserProfileUseCase).should().get(query);
    }

    @Test
    void deleteUser_returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON-200"));

        then(deleteUserUseCase).should().delete(new DeleteUserUseCase.Command(TEST_USER_ID));
    }

    @Test
    void deleteUser_returns404_whenUserNotFound() throws Exception {
        willThrow(new UserException(UserErrorCode.USER_NOT_FOUND))
                .given(deleteUserUseCase)
                .delete(new DeleteUserUseCase.Command(TEST_USER_ID));

        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER-404-1"));
    }

    @Test
    void deleteUser_returns409_whenAlreadyInactive() throws Exception {
        willThrow(new UserException(UserErrorCode.ALREADY_INACTIVE))
                .given(deleteUserUseCase)
                .delete(new DeleteUserUseCase.Command(TEST_USER_ID));

        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER-409-1"));
    }

    private static HandlerMethodArgumentResolver authenticationPrincipalResolver(Long userId) {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                Class<?> type = parameter.getParameterType();
                return parameter.hasParameterAnnotation(
                        org.springframework.security.core.annotation.AuthenticationPrincipal.class
                ) && (Long.class.equals(type) || long.class.equals(type));
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {
                return userId;
            }
        };
    }
}
