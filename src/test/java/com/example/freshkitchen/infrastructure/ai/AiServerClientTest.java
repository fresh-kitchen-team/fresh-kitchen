package com.example.freshkitchen.infrastructure.ai;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                          "bestMatch": "Bibimbap",
                          "confidence": 0.95,
                          "top3": [
                            {"name": "Bibimbap", "confidence": 0.95},
                            {"name": "Fried rice", "confidence": 0.03}
                          ],
                          "source": "gemini"
                        }
                        """, MediaType.APPLICATION_JSON));

        FoodClassificationResponse response = client.classifyFood(imageFile());

        assertEquals("Bibimbap", response.bestMatch());
        assertEquals(0.95, response.confidence());
        assertEquals("Bibimbap", response.top3().get(0).name());
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
                        {"ingredients": ["Egg", "Milk"]}
                        """, MediaType.APPLICATION_JSON));

        ReceiptOcrResponse response = client.extractReceiptIngredients(imageFile());

        assertEquals("Egg", response.ingredients().get(0));
        assertEquals("Milk", response.ingredients().get(1));
        server.verify();
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
                          "objects": [
                            {
                              "name": "Apple",
                              "confidence": 80.0,
                              "box": {"x1": 1, "y1": 2, "x2": 30, "y2": 40}
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        FridgeDetectionResponse response = client.detectFridgeObjects(imageFile());

        assertEquals("Apple", response.objects().get(0).name());
        assertEquals(80.0, response.objects().get(0).confidence());
        assertEquals(30.0, response.objects().get(0).box().x2());
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
    void classifyFood_mapsNullTop3ItemToAiResponseInvalid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServerClient client = new AiServerClient(builder.build(), "service-token");

        server.expect(once(), requestTo("https://ai.example.com/internal/v1/food-classification"))
                .andRespond(withSuccess("""
                        {
                          "bestMatch": "Bibimbap",
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

    private static MockMultipartFile imageFile() {
        return new MockMultipartFile("file", "food.jpg", "image/jpeg", "image".getBytes());
    }
}
