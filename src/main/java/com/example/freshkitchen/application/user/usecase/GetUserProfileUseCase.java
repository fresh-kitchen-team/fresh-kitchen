package com.example.freshkitchen.application.user.usecase;

import com.example.freshkitchen.application.user.dto.UserProfileResult;

public interface GetUserProfileUseCase {

    UserProfileResult get(Query query);

    record Query(
            Long userId
    ) {
    }
}
