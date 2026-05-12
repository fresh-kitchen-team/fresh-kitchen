package com.example.freshkitchen.presentation.ingredient;

import com.example.freshkitchen.application.ingredient.dto.IngredientDto;
import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.GetIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ListIngredientsUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ListStoragesUseCase;
import com.example.freshkitchen.application.ingredient.usecase.ResolveIngredientDefaultsUseCase;
import com.example.freshkitchen.application.ingredient.usecase.UpdateIngredientUseCase;
import com.example.freshkitchen.domain.catalog.enums.CatalogCategory;
import com.example.freshkitchen.domain.ingredient.enums.StorageType;
import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.presentation.ingredient.dto.IngredientRequest;
import com.example.freshkitchen.presentation.ingredient.dto.IngredientResponse;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final CreateIngredientUseCase createIngredientUseCase;
    private final UpdateIngredientUseCase updateIngredientUseCase;
    private final GetIngredientUseCase getIngredientUseCase;
    private final ListIngredientsUseCase listIngredientsUseCase;
    private final ResolveIngredientDefaultsUseCase resolveIngredientDefaultsUseCase;
    private final ListStoragesUseCase listStoragesUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<IngredientResponse.Create>> create(
            @RequestHeader(USER_ID_HEADER) @Positive Long userId,
            @Valid @RequestBody IngredientRequest.Create request
    ) {
        Long ingredientId = createIngredientUseCase.create(request.toCommand(userId));
        return ApiResponse.success(HttpStatus.CREATED, new IngredientResponse.Create(ingredientId));
    }

    @PatchMapping("/{ingredientId}")
    public ResponseEntity<ApiResponse<Void>> update(
            @RequestHeader(USER_ID_HEADER) @Positive Long userId,
            @PathVariable @Positive Long ingredientId,
            @RequestBody JsonNode request
    ) {
        IngredientRequest.Update updateRequest = IngredientRequest.Update.from(request);
        updateIngredientUseCase.update(updateRequest.toCommand(ingredientId, userId));
        return ApiResponse.success();
    }

    @GetMapping("/{ingredientId}")
    public ResponseEntity<ApiResponse<IngredientDto.DetailResponse>> get(
            @RequestHeader(USER_ID_HEADER) @Positive Long userId,
            @PathVariable @Positive Long ingredientId
    ) {
        return ApiResponse.success(
                getIngredientUseCase.get(new GetIngredientUseCase.Query(ingredientId, userId))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<IngredientDto.SummaryResponse>>> list(
            @RequestHeader(USER_ID_HEADER) @Positive Long userId
    ) {
        return ApiResponse.success(
                listIngredientsUseCase.list(new ListIngredientsUseCase.Query(userId))
        );
    }

    @GetMapping("/defaults")
    public ResponseEntity<ApiResponse<IngredientDto.DefaultsResponse>> defaults(
            @RequestParam(required = false) @Positive Long catalogId,
            @RequestParam(required = false) CatalogCategory category,
            @RequestParam(required = false) StorageType storageType
    ) {
        return ApiResponse.success(
                resolveIngredientDefaultsUseCase.resolve(
                        new ResolveIngredientDefaultsUseCase.Query(catalogId, category, storageType)
                )
        );
    }

    @GetMapping("/storages")
    public ResponseEntity<ApiResponse<List<IngredientDto.StorageSummaryResponse>>> storages(
            @RequestHeader(USER_ID_HEADER) @Positive Long userId
    ) {
        return ApiResponse.success(
                listStoragesUseCase.list(new ListStoragesUseCase.Query(userId))
        );
    }
}
