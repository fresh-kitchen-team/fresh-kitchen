package com.example.freshkitchen.infrastructure.ai;

import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.global.exception.CommonErrorCode;
import com.example.freshkitchen.infrastructure.ai.dto.FoodClassificationResponse;
import com.example.freshkitchen.infrastructure.ai.dto.FridgeDetectionResponse;
import com.example.freshkitchen.infrastructure.ai.dto.ReceiptOcrResponse;
import com.example.freshkitchen.infrastructure.ai.exception.AiServerErrorCode;
import com.example.freshkitchen.infrastructure.ai.exception.AiServerException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiServerClientTest {

    @Test
    void classifyFood_mapsActualFastApiSuccessfulResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/food-classification"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andExpect(header("X-Request-Id", org.hamcrest.Matchers.notNullValue()))
                .andRespond(withSuccess("""
                        {
                          "bestMatch": " Bibimbap ",
                          "category": " ETC ",
                          "confidence": 0.95,
                          "top3": [
                            {"name": " Bibimbap ", "confidence": 0.95},
                            {"name": " Fried rice ", "confidence": 0.03}
                          ],
                          "source": " gemini "
                        }
                        """, MediaType.APPLICATION_JSON));

        FoodClassificationResponse response = client.classifyFood(imageFile());

        assertEquals("Bibimbap", response.bestMatch());
        assertEquals("ETC", response.category());
        assertEquals(0.95, response.confidence());
        assertEquals("Bibimbap", response.top3().get(0).name());
        assertEquals("Fried rice", response.top3().get(1).name());
        assertEquals("gemini", response.source());
        assertNull(response.geminiReason());
        assertNull(response.autoSaved());
        server.verify();
    }

    @Test
    void extractReceiptIngredients_mapsSuccessfulResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/receipt-ocr"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "purchasedAt": "2026-05-01",
                          "ingredients": [
                            {"name": " 두부 ", "category": " ETC "},
                            {"name": " 계란 ", "category": " ETC "},
                            {"name": " 김치 ", "category": " VEGETABLE "}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        ReceiptOcrResponse response = client.extractReceiptIngredients(imageFile());

        assertEquals(LocalDate.of(2026, 5, 1), response.purchasedAt());
        assertEquals("두부", response.ingredients().get(0).name());
        assertEquals("ETC", response.ingredients().get(0).category());
        assertEquals("계란", response.ingredients().get(1).name());
        assertEquals("ETC", response.ingredients().get(1).category());
        assertEquals("김치", response.ingredients().get(2).name());
        assertEquals("VEGETABLE", response.ingredients().get(2).category());
        server.verify();
    }

    @Test
    void extractReceiptIngredients_allowsNullPurchasedAtWithRecognizedIngredientsOnly() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/receipt-ocr"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "purchasedAt": null,
                          "ingredients": [
                            {"name": "두부", "category": "ETC"},
                            {"name": "김치", "category": "VEGETABLE"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        ReceiptOcrResponse response = client.extractReceiptIngredients(imageFile());

        assertNull(response.purchasedAt());
        assertEquals("두부", response.ingredients().get(0).name());
        assertEquals("ETC", response.ingredients().get(0).category());
        assertEquals("김치", response.ingredients().get(1).name());
        assertEquals("VEGETABLE", response.ingredients().get(1).category());
        server.verify();
    }

    @Test
    void extractReceiptIngredients_mapsAliasedResponseFields() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/receipt-ocr"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "purchased_at": "2026-05-13",
                          "ingredient_items": [
                            {"name": " 두부 ", "category": " ETC "},
                            {"name": " 고등어 ", "category": " SEAFOOD "}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        ReceiptOcrResponse response = client.extractReceiptIngredients(imageFile());

        assertEquals(LocalDate.of(2026, 5, 13), response.purchasedAt());
        assertEquals("두부", response.ingredients().get(0).name());
        assertEquals("ETC", response.ingredients().get(0).category());
        assertEquals("고등어", response.ingredients().get(1).name());
        assertEquals("SEAFOOD", response.ingredients().get(1).category());
        server.verify();
    }

    @Test
    void extractReceiptIngredients_mapsRecognizedItemsAliases() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/receipt-ocr"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "purchasedAt": "2026-05-13",
                          "recognized_items": [
                            {"name": "두부", "category": "ETC"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        ReceiptOcrResponse response = client.extractReceiptIngredients(imageFile());

        assertEquals(LocalDate.of(2026, 5, 13), response.purchasedAt());
        assertEquals("두부", response.ingredients().get(0).name());
        assertEquals("ETC", response.ingredients().get(0).category());
        server.verify();

        RestClient.Builder camelCaseBuilder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer camelCaseServer = MockRestServiceServer.bindTo(camelCaseBuilder).build();
        AiServerClient camelCaseClient = new AiServerClient(camelCaseBuilder.build(), "service-token");

        camelCaseServer.expect(once(), requestTo("https://ai.example.com/internal/v1/receipt-ocr"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "purchasedAt": "2026-05-14",
                          "recognizedItems": [
                            {"name": "고등어", "category": "SEAFOOD"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        ReceiptOcrResponse camelCaseResponse = camelCaseClient.extractReceiptIngredients(imageFile());

        assertEquals(LocalDate.of(2026, 5, 14), camelCaseResponse.purchasedAt());
        assertEquals("고등어", camelCaseResponse.ingredients().get(0).name());
        assertEquals("SEAFOOD", camelCaseResponse.ingredients().get(0).category());
        camelCaseServer.verify();
    }

    @Test
    void detectFridgeObjects_mapsSuccessfulResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/fridge-detection"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "items": [
                            {"name": " Apple ", "category": " FRUIT "}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        FridgeDetectionResponse response = client.detectFridgeObjects(imageFile());

        assertEquals("Apple", response.items().get(0).name());
        assertEquals("FRUIT", response.items().get(0).category());
        server.verify();
    }

    @Test
    void detectFridgeObjects_mapsDetectedItemsAliases() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/fridge-detection"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "detected_items": [
                            {"name": " 계란 ", "category": " ETC "},
                            {"name": " 우유 ", "category": " DAIRY "}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        FridgeDetectionResponse response = client.detectFridgeObjects(imageFile());

        assertEquals("계란", response.items().get(0).name());
        assertEquals("ETC", response.items().get(0).category());
        assertEquals("우유", response.items().get(1).name());
        assertEquals("DAIRY", response.items().get(1).category());
        server.verify();

        RestClient.Builder camelCaseBuilder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer camelCaseServer = MockRestServiceServer.bindTo(camelCaseBuilder).build();
        AiServerClient camelCaseClient = new AiServerClient(camelCaseBuilder.build(), "service-token");

        camelCaseServer.expect(once(), requestTo("https://ai.example.com/internal/v1/fridge-detection"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "detectedItems": [
                            {"name": " 당근 ", "category": " VEGETABLE "}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        FridgeDetectionResponse camelCaseResponse = camelCaseClient.detectFridgeObjects(imageFile());

        assertEquals("당근", camelCaseResponse.items().get(0).name());
        assertEquals("VEGETABLE", camelCaseResponse.items().get(0).category());
        camelCaseServer.verify();
    }

    @Test
    void detectFridgeObjects_mapsObjectsAlias() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/fridge-detection"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "objects": [
                            {"name": "계란", "category": "ETC"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        FridgeDetectionResponse response = client.detectFridgeObjects(imageFile());

        assertEquals("계란", response.items().get(0).name());
        assertEquals("ETC", response.items().get(0).category());
        server.verify();
    }

    @Test
    void extractReceiptIngredients_mapsMissingIngredientsToAiResponseInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/receipt-ocr"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        AiServerException exception = assertThrows(
                AiServerException.class,
                () -> client.extractReceiptIngredients(imageFile())
        );

        assertEquals(AiServerErrorCode.AI_RESPONSE_INVALID, exception.getErrorCode());
        server.verify();
    }

    @Test
    void extractReceiptIngredients_mapsNullIngredientsToAiResponseInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/receipt-ocr"))
                .andRespond(withSuccess("""
                        {"purchasedAt": "2026-05-01", "ingredients": null}
                        """, MediaType.APPLICATION_JSON));

        AiServerException exception = assertThrows(
                AiServerException.class,
                () -> client.extractReceiptIngredients(imageFile())
        );

        assertEquals(AiServerErrorCode.AI_RESPONSE_INVALID, exception.getErrorCode());
        server.verify();
    }

    @Test
    void extractReceiptIngredients_mapsNullIngredientItemToAiResponseInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/receipt-ocr"))
                .andRespond(withSuccess("""
                        {"purchasedAt": "2026-05-01", "ingredients": [{"name": "두부", "category": "ETC"}, null]}
                        """, MediaType.APPLICATION_JSON));

        AiServerException exception = assertThrows(
                AiServerException.class,
                () -> client.extractReceiptIngredients(imageFile())
        );

        assertEquals(AiServerErrorCode.AI_RESPONSE_INVALID, exception.getErrorCode());
        server.verify();
    }

    @Test
    void extractReceiptIngredients_mapsBlankIngredientItemToAiResponseInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/receipt-ocr"))
                .andRespond(withSuccess("""
                        {"purchasedAt": "2026-05-01", "ingredients": [{"name": " ", "category": "ETC"}]}
                        """, MediaType.APPLICATION_JSON));

        AiServerException exception = assertThrows(
                AiServerException.class,
                () -> client.extractReceiptIngredients(imageFile())
        );

        assertEquals(AiServerErrorCode.AI_RESPONSE_INVALID, exception.getErrorCode());
        server.verify();
    }

    @Test
    void detectFridgeObjects_mapsMissingItemsToAiResponseInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/fridge-detection"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        AiServerException exception = assertThrows(
                AiServerException.class,
                () -> client.detectFridgeObjects(imageFile())
        );

        assertEquals(AiServerErrorCode.AI_RESPONSE_INVALID, exception.getErrorCode());
        server.verify();
    }

    @Test
    void detectFridgeObjects_mapsNullObjectItemToAiResponseInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/fridge-detection"))
                .andRespond(withSuccess("""
                        {"items": [null]}
                        """, MediaType.APPLICATION_JSON));

        AiServerException exception = assertThrows(
                AiServerException.class,
                () -> client.detectFridgeObjects(imageFile())
        );

        assertEquals(AiServerErrorCode.AI_RESPONSE_INVALID, exception.getErrorCode());
        server.verify();
    }

    @Test
    void classifyFood_mapsTimeoutToAiServerTimeout() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://ai.example.com")
                .requestFactory((uri, httpMethod) -> {
                    throw new SocketTimeoutException("read timed out");
                })
                .build();
        AiServerClient client = new AiServerClient(restClient, "service-token");

        AiServerException exception = assertThrows(AiServerException.class, () -> client.classifyFood(imageFile()));

        assertEquals(AiServerErrorCode.AI_SERVER_TIMEOUT, exception.getErrorCode());
    }

    @Test
    void classifyFood_mapsConnectionFailureToAiServerUnavailable() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://ai.example.com")
                .requestFactory((uri, httpMethod) -> {
                    throw new ConnectException("connection refused");
                })
                .build();
        AiServerClient client = new AiServerClient(restClient, "service-token");

        AiServerException exception = assertThrows(AiServerException.class, () -> client.classifyFood(imageFile()));

        assertEquals(AiServerErrorCode.AI_SERVER_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    void classifyFood_mapsServerErrorToAiServerUnavailable() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/food-classification"))
                .andRespond(withServerError());

        AiServerException exception = assertThrows(AiServerException.class, () -> client.classifyFood(imageFile()));

        assertEquals(AiServerErrorCode.AI_SERVER_UNAVAILABLE, exception.getErrorCode());
        server.verify();
    }

    @Test
    void classifyFood_mapsInvalidResponseShapeToAiResponseInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/food-classification"))
                .andRespond(withSuccess("""
                        {"best_match": "Bibimbap", "confidence": 0.95, "top_3": {}, "source": "gemini"}
                        """, MediaType.APPLICATION_JSON));

        AiServerException exception = assertThrows(AiServerException.class, () -> client.classifyFood(imageFile()));

        assertEquals(AiServerErrorCode.AI_RESPONSE_INVALID, exception.getErrorCode());
        server.verify();
    }

    @Test
    void classifyFood_mapsMissingRequiredFieldToAiResponseInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/food-classification"))
                .andRespond(withSuccess("""
                        {"confidence": 0.95, "top_3": [], "source": "gemini"}
                        """, MediaType.APPLICATION_JSON));

        AiServerException exception = assertThrows(AiServerException.class, () -> client.classifyFood(imageFile()));

        assertEquals(AiServerErrorCode.AI_RESPONSE_INVALID, exception.getErrorCode());
        server.verify();
    }

    @Test
    void classifyFood_mapsBlankRequiredStringFieldToAiResponseInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/food-classification"))
                .andRespond(withSuccess("""
                        {
                          "bestMatch": " ",
                          "category": "ETC",
                          "confidence": 0.95,
                          "top3": [{"name": "Bibimbap", "confidence": 0.95}],
                          "source": "gemini"
                        }
                        """, MediaType.APPLICATION_JSON));

        AiServerException exception = assertThrows(AiServerException.class, () -> client.classifyFood(imageFile()));

        assertEquals(AiServerErrorCode.AI_RESPONSE_INVALID, exception.getErrorCode());
        server.verify();
    }

    @Test
    void classifyFood_mapsNullTop3ItemToAiResponseInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/food-classification"))
                .andRespond(withSuccess("""
                        {
                          "bestMatch": "Bibimbap",
                          "category": "ETC",
                          "confidence": 0.95,
                          "top3": [null],
                          "source": "gemini"
                        }
                        """, MediaType.APPLICATION_JSON));

        AiServerException exception = assertThrows(AiServerException.class, () -> client.classifyFood(imageFile()));

        assertEquals(AiServerErrorCode.AI_RESPONSE_INVALID, exception.getErrorCode());
        server.verify();
    }

    @Test
    void classifyFood_mapsBlankTop3NameToAiResponseInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/food-classification"))
                .andRespond(withSuccess("""
                        {
                          "bestMatch": "Bibimbap",
                          "category": "ETC",
                          "confidence": 0.95,
                          "top3": [{"name": " ", "confidence": 0.95}],
                          "source": "gemini"
                        }
                        """, MediaType.APPLICATION_JSON));

        AiServerException exception = assertThrows(AiServerException.class, () -> client.classifyFood(imageFile()));

        assertEquals(AiServerErrorCode.AI_RESPONSE_INVALID, exception.getErrorCode());
        server.verify();
    }

    @Test
    void classifyFood_mapsFastApiUnauthorizedToAiResponseInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/food-classification"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"invalid token\"}"));

        AiServerException exception = assertThrows(AiServerException.class, () -> client.classifyFood(imageFile()));

        assertEquals(AiServerErrorCode.AI_RESPONSE_INVALID, exception.getErrorCode());
        server.verify();
    }

    @Test
    void classifyFood_mapsFastApiValidationErrorToAiResponseInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/food-classification"))
                .andRespond(withStatus(HttpStatusCode.valueOf(422))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":[{\"loc\":[\"body\",\"file\"],\"msg\":\"field required\"}]}"));

        AiServerException exception = assertThrows(AiServerException.class, () -> client.classifyFood(imageFile()));

        assertEquals(AiServerErrorCode.AI_RESPONSE_INVALID, exception.getErrorCode());
        server.verify();
    }

    @Test
    void classifyFood_mapsUnavailableFileResourceToInvalidInput() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://ai.example.com")
                .build();
        AiServerClient client = new AiServerClient(restClient, "service-token");
        MockMultipartFile file = mock(MockMultipartFile.class);
        IllegalStateException cause = new IllegalStateException("file resource unavailable");
        given(file.getResource()).willThrow(cause);

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> client.classifyFood(file)
        );

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.getErrorCode());
        assertSame(cause, exception.getCause());
    }

    @Test
    void classifyFood_rejectsBlankOriginalFilename() {
        AiServerClient client = new AiServerClient(RestClient.builder().build(), "service-token");

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> client.classifyFood(" ", "image".getBytes())
        );

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    @Test
    void extractReceiptIngredients_rejectsNullOriginalFilename() {
        AiServerClient client = new AiServerClient(RestClient.builder().build(), "service-token");

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> client.extractReceiptIngredients(null, "image".getBytes())
        );

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    private static MockMultipartFile imageFile() {
        return new MockMultipartFile("file", "food.jpg", "image/jpeg", "image".getBytes());
    }
}
