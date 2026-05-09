package com.example.freshkitchen.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record FoodClassificationResponse(
        @JsonAlias("best_match")
        String bestMatch,
        Double confidence,
        @JsonAlias({"top_3", "top3"})
        List<FoodCandidate> top3,
        String source,
        @JsonAlias("gemini_reason")
        String geminiReason,
        @JsonAlias("auto_saved")
        Boolean autoSaved
) {

    public record FoodCandidate(
            String name,
            Double confidence
    ) {
    }
}
