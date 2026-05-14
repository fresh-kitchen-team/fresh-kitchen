package com.example.freshkitchen.presentation.image;

import com.example.freshkitchen.application.image.usecase.ChangeIngredientPrimaryImageUseCase;
import com.example.freshkitchen.application.image.usecase.UploadIngredientImageUseCase;
import com.example.freshkitchen.domain.image.enums.ImageContentType;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.presentation.image.dto.ImageRequest;
import com.example.freshkitchen.presentation.image.dto.ImageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ingredients/{ingredientId}/images")
public class ImageController {

    private static final long MAX_UPLOAD_SIZE_BYTES = 10L * 1024 * 1024;

    private final UploadIngredientImageUseCase uploadIngredientImageUseCase;
    private final ChangeIngredientPrimaryImageUseCase changeIngredientPrimaryImageUseCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageResponse.AttachIngredientImage>> uploadIngredientImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long ingredientId,
            @RequestPart("file") MultipartFile file,
            @RequestParam boolean primary,
            @RequestParam IngredientImageSourceType sourceType
    ) {
        validateUploadFile(file);
        UploadIngredientImageUseCase.Result result = uploadIngredientImageUseCase.upload(
                new UploadIngredientImageUseCase.Command(
                        userId,
                        ingredientId,
                        file.getOriginalFilename(),
                        file.getContentType(),
                        bytes(file),
                        primary,
                        sourceType
                )
        );
        return ApiResponse.success(
                HttpStatus.CREATED,
                new ImageResponse.AttachIngredientImage(result.ingredientImageId())
        );
    }

    @PatchMapping("/primary")
    public ResponseEntity<ApiResponse<Void>> changePrimary(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long ingredientId,
            @Valid @RequestBody ImageRequest.ChangePrimary request
    ) {
        changeIngredientPrimaryImageUseCase.change(new ChangeIngredientPrimaryImageUseCase.Command(
                userId,
                ingredientId,
                request.ingredientImageId()
        ));
        return ApiResponse.success();
    }

    private static void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("file must not be empty");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            throw new BusinessValidationException("file size must not exceed 10MB");
        }
        ImageContentType.from(file.getContentType());
    }

    private static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessValidationException("failed to read image file", e);
        }
    }
}
