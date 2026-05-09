package com.example.freshkitchen.domain.chat.controller;

import com.example.freshkitchen.domain.chat.dto.request.ChatMessageRequest;
import com.example.freshkitchen.domain.chat.dto.request.UpdateRoomTitleRequest;
import com.example.freshkitchen.domain.chat.dto.response.ChatMessageResponse;
import com.example.freshkitchen.domain.chat.dto.response.ChatRoomListResponse;
import com.example.freshkitchen.domain.chat.dto.response.ChatRoomResponse;
import com.example.freshkitchen.domain.chat.service.ChatService;
import com.example.freshkitchen.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 채팅", description = "AI 채팅 세션을 통한 조회 대화 등등")
@RestController
@Slf4j
@RequestMapping("/ai/v1/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final ChatService chatService;

    @Operation(summary = "메시지 전송 (AI 레시피 응답)", description = "사용자의 재료와 취향을 바탕으로 AI가 레시피를 응답합니다.")
    @PostMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatMessageRequest request) {

        // CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = 1L;
        ChatMessageResponse response = chatService.sendAiMessage(userId, roomId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "AI 채팅방 생성", description = "AI와 1:1 채팅방을 생성합니다.")
    @PostMapping("/room")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createRoom() {
        // CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = 1L;
        ChatRoomResponse response = chatService.createChatRoom(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "채팅방 목록 조회", description = "사이드바용 채팅방 목록을 날짜별(오늘/7일/30일)로 조회합니다.")
    @GetMapping("/room")
    public ResponseEntity<ApiResponse<ChatRoomListResponse>> getRoomList() {
        // CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = 1L;
        ChatRoomListResponse response = chatService.getRoomList(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "채팅 기록 상세 조회", description = "특정 채팅방의 상세 메시지 내역을 조회합니다.")
    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomListResponse>> getChatHistory(@PathVariable Long roomId) {
        ChatRoomListResponse response = chatService.getChatHistory(roomId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "채팅방 제목 수정", description = "채팅방 제목을 수정합니다.")
    @PatchMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> updateRoomTitle(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomTitleRequest request) {
        ChatRoomResponse response = chatService.updateRoomTitle(roomId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}