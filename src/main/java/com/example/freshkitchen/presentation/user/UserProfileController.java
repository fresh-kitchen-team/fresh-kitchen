package com.example.freshkitchen.presentation.user;

import com.example.freshkitchen.application.user.usecase.GetUserProfileUseCase;
import com.example.freshkitchen.application.user.usecase.DeleteUserProfileUseCase;
import com.example.freshkitchen.application.user.usecase.UpdateUserProfileUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.presentation.user.dto.UserProfileRequest;
import com.example.freshkitchen.presentation.user.dto.UserProfileResponse;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Profile", description = "인증된 사용자의 프로필 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserProfileController {

    private final GetUserProfileUseCase getUserProfileUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final DeleteUserProfileUseCase deleteUserProfileUseCase;

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        UserProfileResponse response = UserProfileResponse.from(
                getUserProfileUseCase.get(new GetUserProfileUseCase.Query(userId))
        );

        return ApiResponse.success(response);
    }

    @Operation(summary = "내 프로필 수정")
    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @RequestBody JsonNode body
    ) {
        UserProfileRequest.Update request = new UserProfileRequest.Update(body);
        updateUserProfileUseCase.update(request.toCommand(userId));
        return ApiResponse.success();
    }

    @Operation(summary = "내 프로필 삭제")
    @DeleteMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        deleteUserProfileUseCase.delete(new DeleteUserProfileUseCase.Command(userId));
        return ApiResponse.success();
    }
}
