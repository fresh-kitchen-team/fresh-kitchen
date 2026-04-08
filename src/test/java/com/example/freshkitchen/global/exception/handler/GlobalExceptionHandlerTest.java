package com.example.freshkitchen.global.exception.handler;

import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
import com.example.freshkitchen.global.exception.BusinessValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TestExceptionController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void handleBusinessException_returnsStandardErrorResponse() throws Exception {
        mockMvc.perform(get("/test-exceptions/domain"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("INGREDIENT-404-1"))
                .andExpect(jsonPath("$.message").value("ingredient not found"))
                .andExpect(jsonPath("$.path").value("/test-exceptions/domain"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void handleIllegalArgumentException_returnsInvalidInputResponse() throws Exception {
        mockMvc.perform(get("/test-exceptions/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/test-exceptions/illegal-argument"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void handleIllegalStateException_returnsInvalidStateResponse() throws Exception {
        mockMvc.perform(get("/test-exceptions/illegal-state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COMMON-409"))
                .andExpect(jsonPath("$.message").value("Invalid state"))
                .andExpect(jsonPath("$.path").value("/test-exceptions/illegal-state"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void handleCommonBusinessException_returnsFixedCommonErrorMessage() throws Exception {
        mockMvc.perform(get("/test-exceptions/business-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON-400"))
                .andExpect(jsonPath("$.message").value("Invalid input"))
                .andExpect(jsonPath("$.path").value("/test-exceptions/business-validation"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void handleUnexpectedRuntimeException_returnsInternalServerErrorResponse() throws Exception {
        mockMvc.perform(get("/test-exceptions/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("COMMON-500"))
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.path").value("/test-exceptions/unexpected"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @RestController
    @RequestMapping("/test-exceptions")
    static class TestExceptionController {

        @GetMapping("/domain")
        String domainException() {
            throw new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND);
        }

        @GetMapping("/illegal-argument")
        String illegalArgumentException() {
            throw new IllegalArgumentException("invalid ingredient input");
        }

        @GetMapping("/illegal-state")
        String illegalStateException() {
            throw new IllegalStateException("ingredient state conflict");
        }

        @GetMapping("/business-validation")
        String businessValidationException() {
            throw new BusinessValidationException("sensitive validation detail");
        }

        @GetMapping("/unexpected")
        String unexpectedException() {
            throw new RuntimeException("boom");
        }
    }
}
