package com.example.freshkitchen.domain.chat.entity;

import com.example.freshkitchen.domain.common.entity.BaseTimeEntity;
import com.example.freshkitchen.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender", nullable = false, length = 50)
    private Sender sender;

    @Column(name = "ai_payload", columnDefinition = "TEXT")
    private String aiPayload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private ChatMessage(String content, Sender sender, String aiPayload, ChatRoom room, User user) {
        this.content = requireNonBlank(content, "content");
        this.sender = requireNonNull(sender, "sender");
        this.aiPayload = aiPayload;
        this.room = room;
        this.user = requireNonNull(user, "user");
    }

    public static ChatMessage create(String content, Sender sender, String aiPayload,
                                     ChatRoom room, User user) {
        return new ChatMessage(content, sender, aiPayload, room, user);
    }
}
