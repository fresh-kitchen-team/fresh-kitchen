package com.example.freshkitchen.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomListResponse {

    private List<ChatMessageGroup> today;
    private List<ChatMessageGroup> last7Days;
    private List<ChatMessageGroup> last30Days;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageGroup {
        private Long roomId;
        private String title;
        private String updatedAt;
        private String sender;   // ✅ 추가
        private String content;
    }
}