package com.example.freshkitchen.application.chat.usecase;

import com.example.freshkitchen.application.chat.dto.ChatRoomResult;

public interface CreateChatRoomUseCase {

    ChatRoomResult create(Command command);

    record Command(
            Long userId,
            String title
    ) {
    }
}
