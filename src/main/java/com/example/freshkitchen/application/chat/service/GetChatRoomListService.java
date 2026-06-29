package com.example.freshkitchen.application.chat.service;

import com.example.freshkitchen.application.chat.dto.ChatRoomResult;
import com.example.freshkitchen.application.chat.usecase.GetChatRoomListUseCase;
import com.example.freshkitchen.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetChatRoomListService implements GetChatRoomListUseCase {

    private final ChatRoomRepository chatRoomRepository;

    @Override
    public List<ChatRoomResult> getAll(Query query) {
        return chatRoomRepository.findAllByUserIdOrderByUpdatedAtDesc(query.userId()).stream()
                .map(ChatRoomResult::from)
                .toList();
    }
}
