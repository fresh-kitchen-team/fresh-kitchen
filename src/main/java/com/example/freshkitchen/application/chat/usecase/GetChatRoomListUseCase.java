package com.example.freshkitchen.application.chat.usecase;

import com.example.freshkitchen.application.chat.dto.ChatRoomResult;

import java.util.List;

public interface GetChatRoomListUseCase {

    List<ChatRoomResult> getAll(Query query);

    record Query(Long userId) {
    }
}
