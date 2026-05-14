package com.example.freshkitchen.domain.chat.repository;

import com.example.freshkitchen.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT r FROM ChatRoom r WHERE r.updatedAt >= :since ORDER BY r.updatedAt DESC")
    List<ChatRoom> findByUpdatedAtAfterOrderByUpdatedAtDesc(OffsetDateTime since);
    List<ChatRoom> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<ChatRoom> findAllByUserIdOrderByUpdatedAtDesc(Long userId);
}
