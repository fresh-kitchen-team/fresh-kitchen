package com.example.freshkitchen.global.exception.handler;

import com.example.freshkitchen.domain.ingredient.exception.IngredientErrorCode;
import com.example.freshkitchen.domain.ingredient.exception.IngredientException;
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
    void handleBaseException_returnsStandardErrorResponse() throws Exception {
        mockMvc.perform(get("/test-exceptions/domain"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("INGREDIENT-404-1"))
                .andExpect(jsonPath("$.message").value("ingredient not found"))
                .andExpect(jsonPath("$.path").value("/test-exceptions/domain"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void handleUnexpectedException_returnsInternalServerErrorResponse() throws Exception {
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

        @GetMapping("/unexpected")
        String unexpectedException() {
            throw new RuntimeException("boom");
        }
    }
}
