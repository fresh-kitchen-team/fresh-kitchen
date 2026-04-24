package com.example.freshkitchen.presentation.user;

import com.example.freshkitchen.application.user.usecase.GetUserProfileUseCase;
import com.example.freshkitchen.presentation.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final GetUserProfileUseCase getUserProfileUseCase;

    @GetMapping("/{userId}/profile")
    public UserProfileResponse getProfile(@PathVariable Long userId) {
        return UserProfileResponse.from(
                getUserProfileUseCase.get(new GetUserProfileUseCase.Query(userId))
        );
    }
}
