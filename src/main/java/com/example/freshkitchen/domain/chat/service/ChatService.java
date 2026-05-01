package com.example.freshkitchen.domain.chat.service;

import com.example.freshkitchen.domain.chat.dto.request.ChatMessageRequest;
import com.example.freshkitchen.domain.chat.dto.request.UpdateRoomTitleRequest;
import com.example.freshkitchen.domain.chat.dto.response.ChatMessageResponse;
import com.example.freshkitchen.domain.chat.dto.response.ChatRoomListResponse;
import com.example.freshkitchen.domain.chat.dto.response.ChatRoomResponse;
import com.example.freshkitchen.domain.chat.entity.AiSetting;
import com.example.freshkitchen.domain.chat.entity.ChatMessage;
import com.example.freshkitchen.domain.chat.entity.ChatRoom;
import com.example.freshkitchen.domain.chat.enums.Sender;
import com.example.freshkitchen.domain.chat.repository.AiSettingRepository;
import com.example.freshkitchen.domain.chat.repository.ChatRoomRepository;
import com.example.freshkitchen.domain.chat.repository.MessageRepository;
import com.example.freshkitchen.domain.ingredient.entity.Ingredient;
import com.example.freshkitchen.domain.ingredient.enums.IngredientStatus;
import com.example.freshkitchen.domain.ingredient.repository.IngredientRepository;
import com.example.freshkitchen.domain.user.entity.User;
import com.example.freshkitchen.domain.user.repository.UserRepository;
import com.example.freshkitchen.global.exception.BusinessException;
import com.example.freshkitchen.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatService {

    private final ChatClient chatClient;
    private final Advisor retrievalAugmentationAdvisor;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final AiSettingRepository aiSettingRepository;
    private final IngredientRepository ingredientRepository;
    /*
        처음 채팅방 생성
     */
    @Transactional
    public ChatRoomResponse createChatRoom(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = ChatRoom.builder()
                .user(user)
                .title("AI 요리비서")
                .build();

        ChatRoom saved = chatRoomRepository.save(chatRoom);

        return ChatRoomResponse.builder()
                .roomId(saved.getId())
                .title(saved.getTitle())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /*
    Rewrite 랑  답변 없으면 LLM에 가져오는 거 추가 시킴
     */
    public ChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, UserRepository userRepository, ChatRoomRepository chatRoomRepository, MessageRepository messageRepository, AiSettingRepository aiSettingRepository, IngredientRepository ingredientRepository) {
        this.userRepository = userRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.messageRepository = messageRepository;
        this.aiSettingRepository = aiSettingRepository;
        this.ingredientRepository = ingredientRepository;

        SimpleLoggerAdvisor customLogger = new SimpleLoggerAdvisor(
                request -> "[SimpleLoggerAdvisor] Custom request: " + request.prompt().getUserMessage(),
                response -> "[SimpleLoggerAdvisor] Custom response: " + response.getResult(),
                Ordered.HIGHEST_PRECEDENCE);

        retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
//                .queryTransformers(RewriteQueryTransformer.builder()
//                        .chatClientBuilder(chatClientBuilder)
//                        .build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .topK(2)
                        .similarityThreshold(0.6)
                        .vectorStore(vectorStore)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();

        this.chatClient = chatClientBuilder
                .defaultAdvisors(customLogger)
                .build();
    }


    /*
     * RAG 기반 스트리밍 응답 (SSE 방식 사용 시 활용)
     */
    public Flux<String> ragChat(String question, String type, String conversationId) {
        return this.chatClient.prompt()
                .system("친절하게 한국어로 답변해줘.")
                .user(question)
                .advisors(retrievalAugmentationAdvisor)
//                .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION,
//                        "type == '%s'".formatted(type)))
                .stream()
                .content();
    }


    /*
    AI 채팅 옵션
     */
    private String buildSystemPrompt(AiSetting setting) {
        StringBuilder sb = new StringBuilder();
        sb.append("친절하게 한국어로 답변해줘.\n");



        // 응답 스타일
        if (setting != null && setting.getResponseStyle() != null) {
            if (setting.getResponseStyle().equals("간단")) {
                sb.append("답변은 간결하고 핵심만 전달해줘.\n");
            } else {
                sb.append("답변은 친절하고 자세하게 설명해줘.\n");
            }
        }

        if (setting != null && setting.isPriorityExpiration()) {
            sb.append("유통기한이 임박한 재료를 우선적으로 사용하는 레시피를 추천해줘.\n");
        }
        if (setting != null && setting.isPriorityNutrition()) {
            sb.append("영양 균형을 고려한 레시피를 추천해줘.\n");
        }
        if (setting != null && setting.isPriorityFrequent()) {
            sb.append("자주 사용하는 재료를 우선으로 활용한 레시피를 추천해줘.\n");
        }
        if (setting != null && setting.isProvideExtraInfo()) {
            sb.append("레시피 외에도 관련 영양 정보, 보관 방법, 요리 팁 등 추가 정보도 함께 제공해줘.\n");
        }

        sb.append("""
            반드시 아래 JSON 형식으로만 응답해:
            {
              "recipes": [
                {
                  "name": "레시피명",
                  "ingredients": ["재료1", "재료2"],
                  "steps": ["단계1", "단계2"],
                  "time": "30분"
                }
              ],
              "tips": ["팁1", "팁2"]
            }
            """);

        return sb.toString();
    }


    @Transactional
    public ChatMessageResponse sendAiMessage(Long userId, Long roomId, ChatMessageRequest request) {


        // AiSetting 조회
        AiSetting setting = aiSettingRepository.findByUserId(userId).orElse(null);
        String systemPrompt = buildSystemPrompt(setting);

        String aiResponseJson = this.chatClient.prompt()
                .system(systemPrompt)
                .user(request.getMessage())
                .advisors(retrievalAugmentationAdvisor)
                .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION,
                        "type == '%s'".formatted(request.getType())))
                .call()
                .content();

        // JSON 파싱
        ObjectMapper objectMapper = new ObjectMapper();
        ChatMessageResponse.AiPayloadResponse aiPayload;
        try {
            aiPayload = objectMapper.readValue(aiResponseJson, ChatMessageResponse.AiPayloadResponse.class);
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패: {}", aiResponseJson);
            aiPayload = ChatMessageResponse.AiPayloadResponse.builder()
                    .recipes(List.of())
                    .tips(List.of())
                    .missingIngredients(List.of())
                    .build();
        }

        // ChatMessage 저장
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .user(chatRoom.getUser())
                .content(aiResponseJson)
                .sender(Sender.AI)
                .aiPayload(aiResponseJson)
                .build();

        ChatMessage saved = messageRepository.save(chatMessage);

        return ChatMessageResponse.builder()
                .title(chatRoom.getTitle())
                .aiMessage(ChatMessageResponse.AiMessageResponse.builder()
                        .messageId(saved.getId())
                        .sender("AI")
                        .text(aiResponseJson)
                        .aiPayload(aiPayload)
                        .createdAt(saved.getCreatedAt().toString())
                        .build())
                .build();
    }


    /*
     채팅 옆에 사이드바 내용물 조회
     */

    @Transactional(readOnly = true)
    public ChatRoomListResponse getRoomList(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository.findByUserIdOrderByUpdatedAtDesc(userId);

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfToday = now.toLocalDate().atStartOfDay().atOffset(now.getOffset());
        OffsetDateTime startOf7Days = now.minusDays(7);
        OffsetDateTime startOf30Days = now.minusDays(30);

        List<ChatRoomListResponse.ChatMessageGroup> today = rooms.stream()
                .filter(r -> r.getUpdatedAt().isAfter(startOfToday))
                .map(this::toChatMessageGroup)
                .collect(Collectors.toList());

        List<ChatRoomListResponse.ChatMessageGroup> last7Days = rooms.stream()
                .filter(r -> r.getUpdatedAt().isAfter(startOf7Days) && r.getUpdatedAt().isBefore(startOfToday))
                .map(this::toChatMessageGroup)
                .collect(Collectors.toList());

        List<ChatRoomListResponse.ChatMessageGroup> last30Days = rooms.stream()
                .filter(r -> r.getUpdatedAt().isAfter(startOf30Days) && r.getUpdatedAt().isBefore(startOf7Days))
                .map(this::toChatMessageGroup)
                .collect(Collectors.toList());

        return ChatRoomListResponse.builder()
                .today(today)
                .last7Days(last7Days)
                .last30Days(last30Days)
                .build();
    }

    private ChatRoomListResponse.ChatMessageGroup toChatMessageGroup(ChatRoom room) {
        return ChatRoomListResponse.ChatMessageGroup.builder()
                .roomId(room.getId())
                .title(room.getTitle())
                .updatedAt(room.getUpdatedAt().toString())
                .build();
    }

    /*
     채팅 옆에 사이드바 조회
     */
    @Transactional(readOnly = true)
    public ChatRoomListResponse getChatHistory(Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        List<ChatRoomListResponse.ChatMessageGroup> messages = chatRoom.getMessages().stream()
                .map(m -> ChatRoomListResponse.ChatMessageGroup.builder()
                        .roomId(chatRoom.getId())
                        .title(chatRoom.getTitle())
                        .updatedAt(m.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());

        return ChatRoomListResponse.builder()
                .today(messages)
                .last7Days(List.of())
                .last30Days(List.of())
                .build();
    }

    @Transactional
    public ChatRoomResponse updateRoomTitle(Long roomId, UpdateRoomTitleRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        chatRoom.updateTitle(request.getTitle());

        return ChatRoomResponse.builder()
                .roomId(chatRoom.getId())
                .title(chatRoom.getTitle())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}