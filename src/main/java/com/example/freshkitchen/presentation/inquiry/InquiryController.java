package com.example.freshkitchen.presentation.inquiry;

import com.example.freshkitchen.application.inquiry.usecase.SendInquiryUseCase;
import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.presentation.inquiry.dto.InquiryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final SendInquiryUseCase sendInquiryUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> send(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody InquiryRequest request
    ) {
        sendInquiryUseCase.send(new SendInquiryUseCase.Command(
                userId,
                request.title(),
                request.content(),
                request.contactEmail()
        ));
        return ApiResponse.success();
    }
}
