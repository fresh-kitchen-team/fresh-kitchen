package com.example.freshkitchen.application.chat.service;

import com.example.freshkitchen.application.chat.dto.ChatMessageResult;
import com.example.freshkitchen.application.chat.usecase.SendChatMessageUseCase;
import com.example.freshkitchen.domain.chat.entity.AiSetting;
import com.example.freshkitchen.domain.chat.entity.ChatMessage;
import com.example.freshkitchen.domain.chat.entity.ChatRoom;
import com.example.freshkitchen.domain.chat.entity.Sender;
import com.example.freshkitchen.domain.chat.exception.ChatErrorCode;
import com.example.freshkitchen.domain.chat.exception.ChatException;
import com.example.freshkitchen.domain.chat.repository.AiSettingRepository;
import com.example.freshkitchen.domain.chat.repository.ChatMessageRepository;
import com.example.freshkitchen.domain.chat.repository.ChatRoomRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.infrastructure.gemini.GeminiChatClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SendChatMessageService implements SendChatMessageUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiSettingRepository aiSettingRepository;
    private final UserRepository userRepository;
    private final GeminiChatClient geminiChatClient;

    @Override
    public ChatMessageResult send(Command command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.USER_NOT_FOUND));

        ChatRoom room = chatRoomRepository.findById(command.roomId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!room.getUser().getId().equals(command.userId())) {
            throw new ChatException(ChatErrorCode.CHAT_ROOM_NOT_OWNED_BY_USER);
        }

        // Save user message
        ChatMessage userMessage = ChatMessage.create(
                command.message(), Sender.USER, null, room, user
        );
        chatMessageRepository.save(userMessage);

        // Build prompt with AI settings context
        String prompt = buildPrompt(command.message(), command.userId());

        // Call Gemini
        String aiResponse;
        try {
            aiResponse = geminiChatClient.chat(prompt);
        } catch (Exception e) {
            log.error("Gemini API call failed for roomId={}, userId={}", command.roomId(), command.userId(), e);
            throw new ChatException(ChatErrorCode.AI_RESPONSE_PARSE_FAILED);
        }

        // Save AI response
        ChatMessage aiMessage = ChatMessage.create(
                aiResponse, Sender.AI, null, room, user
        );
        chatMessageRepository.save(aiMessage);

        return ChatMessageResult.from(aiMessage);
    }

    private String buildPrompt(String userMessage, Long userId) {
        AiSetting setting = aiSettingRepository.findByUserId(userId).orElse(null);

        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful kitchen assistant. ");

        if (setting != null) {
            if (setting.getResponseStyle() != null) {
                sb.append("Response style: ").append(setting.getResponseStyle()).append(". ");
            }
            if (setting.isPriorityExpiration()) {
                sb.append("Prioritize ingredients nearing expiration. ");
            }
            if (setting.isPriorityNutrition()) {
                sb.append("Consider nutritional balance. ");
            }
        }

        sb.append("\n\nUser: ").append(userMessage);
        return sb.toString();
    }
}
