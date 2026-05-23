package com.example.freshkitchen.presentation.inquiry;

import com.example.freshkitchen.application.inquiry.usecase.GetInquiryDetailUseCase;
import com.example.freshkitchen.application.inquiry.usecase.GetInquiryListUseCase;
import com.example.freshkitchen.application.inquiry.usecase.InquiryCategory;
import com.example.freshkitchen.application.inquiry.usecase.InquiryType;
import com.example.freshkitchen.application.inquiry.usecase.SendInquiryUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.presentation.inquiry.dto.InquiryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Inquiry", description = "문의/신고 API")
@Validated
@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final SendInquiryUseCase sendInquiryUseCase;
    private final GetInquiryListUseCase getInquiryListUseCase;
    private final GetInquiryDetailUseCase getInquiryDetailUseCase;

    @Operation(summary = "문의/신고 등록", description = "문의 또는 신고를 등록하고 개발팀에 이메일을 발송합니다")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InquiryResponse.Created>> send(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,

            @RequestParam
            @Schema(description = "문의 유형 (INQUIRY: 문의, REPORT: 신고)", example = "INQUIRY")
            InquiryType type,

            @RequestParam
            @Schema(description = "카테고리 (RECIPE: 레시피 관련, AI: AI 관련, OTHER: 기타)", example = "RECIPE")
            InquiryCategory category,

            @RequestParam
            @NotBlank(message = "내용은 필수입니다")
            @Size(max = 5000, message = "내용은 5000자 이내여야 합니다")
            @Schema(description = "문의/신고 내용", example = "레시피 추천이 잘못된 것 같습니다.")
            String content,

            @RequestPart(value = "image", required = false)
            @Schema(description = "첨부 이미지 (선택)")
            MultipartFile image
    ) {
        Long inquiryId = sendInquiryUseCase.send(new SendInquiryUseCase.Command(
                userId, type, category, content, image
        ));
        return ApiResponse.success(new InquiryResponse.Created(inquiryId));
    }

    @Operation(summary = "내 문의 목록", description = "로그인한 사용자의 문의/신고 내역을 최신순으로 조회합니다")
    @GetMapping
    public ResponseEntity<ApiResponse<List<InquiryResponse.Summary>>> getList(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                InquiryResponse.Summary.from(
                        getInquiryListUseCase.getList(new GetInquiryListUseCase.Command(userId))
                )
        );
    }

    @Operation(summary = "문의 상세 조회", description = "문의 내용과 관리자 답변을 조회합니다")
    @GetMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<InquiryResponse.Detail>> getDetail(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long inquiryId
    ) {
        return ApiResponse.success(
                InquiryResponse.Detail.from(
                        getInquiryDetailUseCase.getDetail(
                                new GetInquiryDetailUseCase.Command(userId, inquiryId)
                        )
                )
        );
    }
}
