package com.example.freshkitchen.application.user.usecase;

public interface DeleteUserProfileUseCase {

    void delete(Command command);

    record Command(
            Long userId
    ) {
    }
}
