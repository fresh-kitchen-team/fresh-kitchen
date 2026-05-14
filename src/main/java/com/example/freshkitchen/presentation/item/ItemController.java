package com.example.freshkitchen.presentation.item;

import com.example.freshkitchen.application.image.usecase.AttachIngredientImageUseCase;
import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.DeleteIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.GetIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ListStoragesUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ListIngredientsUseCase;
import com.example.freshkitchen.application.ingredient.usecase.UpdateIngredientUseCase;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.presentation.item.dto.ItemRequest;
import com.example.freshkitchen.presentation.item.dto.ItemResponse;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final CreateIngredientUseCase createIngredientUseCase;
    private final UpdateIngredientUseCase updateIngredientUseCase;
    private final GetIngredientUseCase getIngredientUseCase;
    private final ListIngredientsUseCase listIngredientsUseCase;
    private final DeleteIngredientUseCase deleteIngredientUseCase;
    private final ListStoragesUseCase listStoragesUseCase;
    private final AttachIngredientImageUseCase attachIngredientImageUseCase;
    private final Clock clock;

    @PostMapping
    public ResponseEntity<ApiResponse<ItemResponse.Create>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ItemRequest.Create request
    ) {
        Long itemId = createIngredientUseCase.create(request.toCommand(userId, LocalDate.now(clock)));
        if (request.imageAssetId() != null) {
            attachIngredientImageUseCase.attach(new AttachIngredientImageUseCase.Command(
                    userId,
                    itemId,
                    request.imageAssetId(),
                    true,
                    IngredientImageSourceType.PHOTO
            ));
        }
        return ApiResponse.success(HttpStatus.CREATED, new ItemResponse.Create(itemId));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<ItemResponse.Item>> get(
            @AuthenticationPrincipal Long userId,
            @PathVariable @Positive Long itemId
    ) {
        IngredientDto.DetailResponse response = getIngredientUseCase.get(new GetIngredientUseCase.Query(itemId, userId));
        return ApiResponse.success(ItemResponse.Item.from(response, clock));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ItemResponse.Item>>> list(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(ItemResponse.fromSummaries(
                listIngredientsUseCase.list(new ListIngredientsUseCase.Query(userId)),
                clock
        ));
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<ApiResponse<Void>> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable @Positive Long itemId,
            @RequestBody JsonNode request
    ) {
        updateIngredientUseCase.update(ItemRequest.Update.from(request).toCommand(itemId, userId));
        return ApiResponse.success();
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable @Positive Long itemId
    ) {
        deleteIngredientUseCase.delete(new DeleteIngredientUseCase.Command(itemId, userId));
        return ApiResponse.success();
    }

    @GetMapping("/storages")
    public ResponseEntity<ApiResponse<List<IngredientDto.StorageSummaryResponse>>> storages(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                listStoragesUseCase.list(new ListStoragesUseCase.Query(userId))
        );
    }
}
