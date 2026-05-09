package com.example.freshkitchen.domain.chat.dto.request;

// UpdateRoomTitleRequest.java

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRoomTitleRequest {

    @NotBlank
    private String title;
}