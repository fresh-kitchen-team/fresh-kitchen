package com.example.freshkitchen.application.user.usecase;

public interface HardDeleteUserUseCase {

    void delete(Command command);

    record Command(Long userId) {
    }
}
