package com.example.freshkitchen.domain.chat.dto.request;

// UpdateRoomTitleRequest.java

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRoomTitleRequest {

    @NotBlank
    private String title;
}