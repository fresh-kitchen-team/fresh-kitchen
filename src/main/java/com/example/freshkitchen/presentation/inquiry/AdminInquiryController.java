package com.example.freshkitchen.presentation.inquiry;

import com.example.freshkitchen.application.inquiry.usecase.ReplyInquiryUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Inquiry", description = "관리자 문의 답변 API (Swagger에서 사용)")
@RestController
@RequestMapping("/api/v1/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final ReplyInquiryUseCase replyInquiryUseCase;

    @Operation(
            summary = "문의 답변",
            description = "메일로 받은 문의 ID에 대해 답변을 등록합니다. 답변 등록 시 사용자에게 FCM 푸시가 발송됩니다."
    )
    @PutMapping("/{inquiryId}/reply")
    public ResponseEntity<ApiResponse<Void>> reply(
            @PathVariable Long inquiryId,
            @Valid @RequestBody ReplyRequest request
    ) {
        replyInquiryUseCase.reply(new ReplyInquiryUseCase.Command(inquiryId, request.reply()));
        return ApiResponse.success();
    }

    public record ReplyRequest(
            @NotBlank(message = "답변 내용은 필수입니다")
            @Size(max = 5000, message = "답변은 5000자 이내여야 합니다")
            String reply
    ) {
    }
}
