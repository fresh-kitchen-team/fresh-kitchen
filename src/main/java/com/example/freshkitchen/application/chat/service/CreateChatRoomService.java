package com.example.freshkitchen.application.chat.service;

import com.example.freshkitchen.application.chat.dto.ChatRoomResult;
import com.example.freshkitchen.application.chat.usecase.CreateChatRoomUseCase;
import com.example.freshkitchen.domain.chat.entity.ChatRoom;
import com.example.freshkitchen.domain.chat.exception.ChatErrorCode;
import com.example.freshkitchen.domain.chat.exception.ChatException;
import com.example.freshkitchen.domain.chat.repository.ChatRoomRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateChatRoomService implements CreateChatRoomUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Override
    public ChatRoomResult create(Command command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.USER_NOT_FOUND));

        ChatRoom room = ChatRoom.create(command.title(), user);
        chatRoomRepository.save(room);
        return ChatRoomResult.from(room);
    }
}
