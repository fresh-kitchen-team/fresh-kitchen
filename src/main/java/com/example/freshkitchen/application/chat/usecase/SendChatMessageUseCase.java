package com.example.freshkitchen.application.chat.usecase;

import com.example.freshkitchen.application.chat.dto.ChatMessageResult;

public interface SendChatMessageUseCase {

    ChatMessageResult send(Command command);

    record Command(
            Long userId,
            Long roomId,
            String message
    ) {
    }
}
