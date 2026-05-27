package com.example.freshkitchen.application.user.usecase;

public interface DeleteUserUseCase {

    void delete(Command command);

    record Command(Long userId) {
    }
}
