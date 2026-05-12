package com.example.freshkitchen.presentation.image;

import com.example.freshkitchen.application.image.usecase.ChangeIngredientPrimaryImageUseCase;
import com.example.freshkitchen.application.image.usecase.UploadIngredientImageUseCase;
import com.example.freshkitchen.domain.image.enums.IngredientImageSourceType;
import com.example.freshkitchen.global.exception.handler.GlobalExceptionHandler;
import com.example.freshkitchen.global.security.JwtAuthentication;
import com.example.freshkitchen.global.security.Role;
import com.example.freshkitchen.presentation.image.dto.ImageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerTest {

    private UploadIngredientImageUseCase uploadIngredientImageUseCase;
    private ChangeIngredientPrimaryImageUseCase changeIngredientPrimaryImageUseCase;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        uploadIngredientImageUseCase = mock(UploadIngredientImageUseCase.class);
        changeIngredientPrimaryImageUseCase = mock(ChangeIngredientPrimaryImageUseCase.class);
        objectMapper = new ObjectMapper();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ImageController(uploadIngredientImageUseCase, changeIngredientPrimaryImageUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadIngredientImage_returnsWrappedCreatedResponse() throws Exception {
        when(uploadIngredientImageUseCase.upload(any(UploadIngredientImageUseCase.Command.class)))
                .thenReturn(new UploadIngredientImageUseCase.Result(30L));

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthentication(1L, Role.USER));

        mockMvc.perform(multipart("/api/v1/ingredients/10/images")
                        .file(imageFile())
                        .param("primary", "true")
                        .param("sourceType", "PHOTO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("COMMON-201"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.ingredientImageId").value(30));

        ArgumentCaptor<UploadIngredientImageUseCase.Command> captor =
                ArgumentCaptor.forClass(UploadIngredientImageUseCase.Command.class);
        verify(uploadIngredientImageUseCase).upload(captor.capture());
        UploadIngredientImageUseCase.Command command = captor.getValue();
        assertAll(
                () -> assertEquals(1L, command.userId()),
                () -> assertEquals(10L, command.ingredientId()),
                () -> assertEquals("tomato.jpg", command.originalFilename()),
                () -> assertEquals("image/jpeg", command.contentType()),
                () -> assertArrayEquals("image".getBytes(), command.content()),
                () -> assertEquals(true, command.primary()),
                () -> assertEquals(IngredientImageSourceType.PHOTO, command.sourceType())
        );
    }

    @Test
    void changePrimary_returnsWrappedSuccessResponse() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthentication(1L, Role.USER));

        mockMvc.perform(patch("/api/v1/ingredients/10/images/primary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ImageRequest.ChangePrimary(30L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data").doesNotExist());

        ArgumentCaptor<ChangeIngredientPrimaryImageUseCase.Command> captor =
                ArgumentCaptor.forClass(ChangeIngredientPrimaryImageUseCase.Command.class);
        verify(changeIngredientPrimaryImageUseCase).change(captor.capture());
        assertAll(
                () -> assertEquals(1L, captor.getValue().userId()),
                () -> assertEquals(10L, captor.getValue().ingredientId()),
                () -> assertEquals(30L, captor.getValue().ingredientImageId())
        );
    }

    private static MockMultipartFile imageFile() {
        return new MockMultipartFile("file", "tomato.jpg", "image/jpeg", "image".getBytes());
    }
}
