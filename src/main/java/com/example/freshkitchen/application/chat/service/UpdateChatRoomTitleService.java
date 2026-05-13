package com.example.freshkitchen.application.chat.service;

import com.example.freshkitchen.application.chat.usecase.UpdateChatRoomTitleUseCase;
import com.example.freshkitchen.domain.chat.entity.ChatRoom;
import com.example.freshkitchen.domain.chat.exception.ChatErrorCode;
import com.example.freshkitchen.domain.chat.exception.ChatException;
import com.example.freshkitchen.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateChatRoomTitleService implements UpdateChatRoomTitleUseCase {

    private final ChatRoomRepository chatRoomRepository;

    @Override
    public void update(Command command) {
        ChatRoom room = chatRoomRepository.findById(command.roomId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!room.getUser().getId().equals(command.userId())) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_NOT_OWNED_BY_USER);
        }

        room.updateTitle(command.title());
    }
}
