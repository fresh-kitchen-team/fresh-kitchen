package com.example.freshkitchen.application.chat.service;

import com.example.freshkitchen.application.chat.dto.ChatMessageResult;
import com.example.freshkitchen.application.chat.usecase.GetChatHistoryUseCase;
import com.example.freshkitchen.domain.chat.entity.ChatRoom;
import com.example.freshkitchen.domain.chat.exception.ChatErrorCode;
import com.example.freshkitchen.domain.chat.exception.ChatException;
import com.example.freshkitchen.domain.chat.repository.ChatMessageRepository;
import com.example.freshkitchen.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetChatHistoryService implements GetChatHistoryUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    public List<ChatMessageResult> getHistory(Query query) {
        ChatRoom room = chatRoomRepository.findById(query.roomId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!room.getUser().getId().equals(query.userId())) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_NOT_OWNED_BY_USER);
        }

        return chatMessageRepository.findAllByRoomIdOrderByCreatedAtAsc(query.roomId()).stream()
                .map(ChatMessageResult::from)
                .toList();
    }
}
