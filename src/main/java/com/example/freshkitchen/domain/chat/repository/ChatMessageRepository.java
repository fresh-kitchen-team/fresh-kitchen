package com.example.freshkitchen.domain.chat.repository;

import com.example.freshkitchen.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllByRoomIdOrderByCreatedAtAsc(Long roomId);
    Long countByRoomId(Long roomId);

}
