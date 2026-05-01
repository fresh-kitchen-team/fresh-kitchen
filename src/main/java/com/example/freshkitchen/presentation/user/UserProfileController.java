package com.example.freshkitchen.presentation.user;

import com.example.freshkitchen.application.user.usecase.GetUserProfileUseCase;
import com.example.freshkitchen.application.user.usecase.DeleteUserProfileUseCase;
import com.example.freshkitchen.application.user.usecase.UpdateUserProfileUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.presentation.user.dto.UserProfileRequest;
import com.example.freshkitchen.presentation.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final GetUserProfileUseCase getUserProfileUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final DeleteUserProfileUseCase deleteUserProfileUseCase;

    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(@PathVariable Long userId) {
        UserProfileResponse response = UserProfileResponse.from(
                getUserProfileUseCase.get(new GetUserProfileUseCase.Query(userId))
        );

        return ApiResponse.success(response);
    }

    @PatchMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @PathVariable Long userId,
            @RequestBody JsonNode body
    ) {
        UserProfileRequest.Update request = new UserProfileRequest.Update(body);
        updateUserProfileUseCase.update(request.toCommand(userId));
        return ApiResponse.success();
    }

    @DeleteMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(@PathVariable Long userId) {
        deleteUserProfileUseCase.delete(new DeleteUserProfileUseCase.Command(userId));
        return ApiResponse.success();
    }
}
