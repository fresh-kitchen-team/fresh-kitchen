package com.example.freshkitchen.presentation.chat;

import com.example.freshkitchen.application.chat.usecase.CreateChatRoomUseCase;
import com.example.freshkitchen.application.chat.usecase.GetChatHistoryUseCase;
import com.example.freshkitchen.application.chat.usecase.GetChatRoomListUseCase;
import com.example.freshkitchen.application.chat.usecase.SendChatMessageUseCase;
import com.example.freshkitchen.application.chat.usecase.UpdateChatRoomTitleUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.presentation.chat.dto.ChatRequest;
import com.example.freshkitchen.presentation.chat.dto.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Chat", description = "AI 챗봇 채팅방 및 메시지 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final CreateChatRoomUseCase createChatRoomUseCase;
    private final GetChatRoomListUseCase getChatRoomListUseCase;
    private final GetChatHistoryUseCase getChatHistoryUseCase;
    private final SendChatMessageUseCase sendChatMessageUseCase;
    private final UpdateChatRoomTitleUseCase updateChatRoomTitleUseCase;

    @Operation(summary = "채팅방 생성")
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatResponse.Room>> createRoom(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChatRequest.CreateRoom request
    ) {
        ChatResponse.Room response = ChatResponse.Room.from(
                createChatRoomUseCase.create(new CreateChatRoomUseCase.Command(userId, request.title()))
        );
        return ApiResponse.success(HttpStatus.CREATED, response);
    }

    @Operation(summary = "채팅방 목록 조회")
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatResponse.Room>>> getRooms(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        List<ChatResponse.Room> response = getChatRoomListUseCase
                .getAll(new GetChatRoomListUseCase.Query(userId)).stream()
                .map(ChatResponse.Room::from)
                .toList();
        return ApiResponse.success(response);
    }

    @Operation(summary = "채팅 내역 조회")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatResponse.Message>>> getHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId
    ) {
        List<ChatResponse.Message> response = getChatHistoryUseCase
                .getHistory(new GetChatHistoryUseCase.Query(userId, roomId)).stream()
                .map(ChatResponse.Message::from)
                .toList();
        return ApiResponse.success(response);
    }

    @Operation(summary = "메시지 전송 (AI 응답 포함)")
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<ChatResponse.Message>> sendMessage(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId,
            @Valid @RequestBody ChatRequest.SendMessage request
    ) {
        ChatResponse.Message response = ChatResponse.Message.from(
                sendChatMessageUseCase.send(
                        new SendChatMessageUseCase.Command(userId, roomId, request.message())
                )
        );
        return ApiResponse.success(HttpStatus.CREATED, response);
    }

    @Operation(summary = "채팅방 제목 수정")
    @PatchMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<Void>> updateRoomTitle(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId,
            @Valid @RequestBody ChatRequest.UpdateRoomTitle request
    ) {
        updateChatRoomTitleUseCase.update(
                new UpdateChatRoomTitleUseCase.Command(userId, roomId, request.title())
        );
        return ApiResponse.success();
    }
}
