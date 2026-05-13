package com.example.freshkitchen.application.chat.usecase;

import com.example.freshkitchen.application.chat.dto.ChatMessageResult;

import java.util.List;

public interface GetChatHistoryUseCase {

    List<ChatMessageResult> getHistory(Query query);

    record Query(
            Long userId,
            Long roomId
    ) {
    }
}
