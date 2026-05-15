package com.example.freshkitchen.application.ingredient.service;

import com.example.freshkitchen.application.image.usecase.AttachIngredientImageUseCase;
import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientUseCase;
import com.example.freshkitchen.application.ingredient.usecase.CreateIngredientWithImageUseCase;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.domain.image.exception.ImageErrorCode;
import com.example.freshkitchen.domain.image.exception.ImageException;
import com.example.freshkitchen.domain.ingredient.enums.ExpirySourceType;
import com.example.freshkitchen.domain.ingredient.enums.IngredientSourceType;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateIngredientWithImageServiceTest {

    private final CreateIngredientUseCase createIngredientUseCase = mock(CreateIngredientUseCase.class);
    private final AttachIngredientImageUseCase attachIngredientImageUseCase = mock(AttachIngredientImageUseCase.class);
    private final CreateIngredientWithImageService service = new CreateIngredientWithImageService(
            createIngredientUseCase,
            attachIngredientImageUseCase
    );

    @Test
    void create_createsIngredientAndAttachesImageInTransactionalService() {
        CreateIngredientUseCase.Command ingredientCommand = command();
        when(createIngredientUseCase.create(ingredientCommand)).thenReturn(10L);

        Long ingredientId = service.create(new CreateIngredientWithImageUseCase.Command(ingredientCommand, 20L));

        ArgumentCaptor<AttachIngredientImageUseCase.Command> attachCaptor =
                ArgumentCaptor.forClass(AttachIngredientImageUseCase.Command.class);
        verify(attachIngredientImageUseCase).attach(attachCaptor.capture());
        assertAll(
                () -> assertEquals(10L, ingredientId),
                () -> assertEquals(1L, attachCaptor.getValue().userId()),
                () -> assertEquals(10L, attachCaptor.getValue().ingredientId()),
                () -> assertEquals(20L, attachCaptor.getValue().imageAssetId()),
                () -> assertTrue(attachCaptor.getValue().primary()),
                () -> assertEquals(IngredientImageSourceType.PHOTO, attachCaptor.getValue().sourceType()),
                () -> assertTrue(CreateIngredientWithImageService.class.isAnnotationPresent(Transactional.class))
        );
    }

    @Test
    void create_skipsAttachWhenImageAssetIdIsNull() {
        CreateIngredientUseCase.Command ingredientCommand = command();
        when(createIngredientUseCase.create(ingredientCommand)).thenReturn(10L);

        Long ingredientId = service.create(new CreateIngredientWithImageUseCase.Command(ingredientCommand, null));

        assertEquals(10L, ingredientId);
        verify(attachIngredientImageUseCase, never()).attach(any());
    }

    @Test
    void create_propagatesAttachFailureForTransactionRollback() {
        CreateIngredientUseCase.Command ingredientCommand = command();
        ImageException attachFailure = new ImageException(ImageErrorCode.IMAGE_ASSET_NOT_FOUND);
        when(createIngredientUseCase.create(ingredientCommand)).thenReturn(10L);
        when(attachIngredientImageUseCase.attach(any(AttachIngredientImageUseCase.Command.class)))
                .thenThrow(attachFailure);

        ImageException exception = assertThrows(
                ImageException.class,
                () -> service.create(new CreateIngredientWithImageUseCase.Command(ingredientCommand, 20L))
        );

        assertSame(attachFailure, exception);
    }

    @Test
    void create_rejectsMissingCommand() {
        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> service.create(null)
        );

        assertEquals("command must not be null", exception.getMessage());
    }

    @Test
    void create_rejectsMissingIngredientCommand() {
        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> service.create(new CreateIngredientWithImageUseCase.Command(null, 20L))
        );

        assertEquals("ingredientCommand must not be null", exception.getMessage());
    }

    private static CreateIngredientUseCase.Command command() {
        return new CreateIngredientUseCase.Command(
                1L,
                2L,
                3L,
                "Tomato",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 6),
                ExpirySourceType.MANUAL,
                "salad",
                IngredientSourceType.PHOTO
        );
    }
}
