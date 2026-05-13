package com.example.freshkitchen.application.chat.usecase;

public interface UpdateChatRoomTitleUseCase {

    void update(Command command);

    record Command(
            Long userId,
            Long roomId,
            String title
    ) {
    }
}
