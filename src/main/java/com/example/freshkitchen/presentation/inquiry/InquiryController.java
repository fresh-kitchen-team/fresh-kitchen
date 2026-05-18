package com.example.freshkitchen.presentation.inquiry;

import com.example.freshkitchen.application.inquiry.usecase.InquiryCategory;
import com.example.freshkitchen.application.inquiry.usecase.InquiryType;
import com.example.freshkitchen.application.inquiry.usecase.SendInquiryUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final SendInquiryUseCase sendInquiryUseCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> send(
            @AuthenticationPrincipal Long userId,

            @RequestParam
            @Schema(description = "문의 유형 (INQUIRY: 문의, REPORT: 신고)", example = "INQUIRY")
            InquiryType type,

            @RequestParam
            @Schema(description = "카테고리 (RECIPE: 레시피 관련, AI: AI 관련, OTHER: 기타)", example = "RECIPE")
            InquiryCategory category,

            @RequestParam
            @Schema(description = "문의/신고 내용", example = "레시피 추천이 잘못된 것 같습니다.")
            String content,

            @RequestPart(value = "image", required = false)
            @Schema(description = "첨부 이미지 (선택)")
            MultipartFile image
    ) {
        sendInquiryUseCase.send(new SendInquiryUseCase.Command(
                userId, type, category, content, image
        ));
        return ApiResponse.success();
    }
}
