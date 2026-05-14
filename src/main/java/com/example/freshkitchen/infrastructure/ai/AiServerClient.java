package com.example.freshkitchen.infrastructure.ai;

import com.example.freshkitchen.global.exception.BusinessValidationException;
import com.example.freshkitchen.infrastructure.ai.dto.FoodClassificationResponse;
import com.example.freshkitchen.infrastructure.ai.dto.FridgeDetectionResponse;
import com.example.freshkitchen.infrastructure.ai.dto.ReceiptOcrResponse;
import com.example.freshkitchen.infrastructure.ai.exception.AiServerErrorCode;
import com.example.freshkitchen.infrastructure.ai.exception.AiServerException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.UUID;

@Component
public class AiServerClient {

    private static final String FOOD_CLASSIFICATION_PATH = "/internal/v1/food-classification";
    private static final String RECEIPT_OCR_PATH = "/internal/v1/receipt-ocr";
    private static final String FRIDGE_DETECTION_PATH = "/internal/v1/fridge-detection";

    private final RestClient restClient;
    private final String token;

    @Autowired
    public AiServerClient(AiServerProperties properties) {
        this(createRestClient(properties), properties.getToken());
    }

    AiServerClient(RestClient restClient, String token) {
        this.restClient = restClient;
        this.token = token;
    }

    public FoodClassificationResponse classifyFood(MultipartFile file) {
        FoodClassificationResponse response = postMultipart(FOOD_CLASSIFICATION_PATH, file, FoodClassificationResponse.class);
        validateFoodClassification(response);
        return response;
    }

    public FoodClassificationResponse classifyFood(String originalFilename, byte[] content) {
        FoodClassificationResponse response = postMultipart(
                FOOD_CLASSIFICATION_PATH,
                multipartBody(originalFilename, content),
                FoodClassificationResponse.class
        );
        validateFoodClassification(response);
        return response;
    }

    public ReceiptOcrResponse extractReceiptIngredients(MultipartFile file) {
        ReceiptOcrResponse response = postMultipart(RECEIPT_OCR_PATH, file, ReceiptOcrResponse.class);
        validateReceiptOcr(response);
        return response;
    }

    public ReceiptOcrResponse extractReceiptIngredients(String originalFilename, byte[] content) {
        ReceiptOcrResponse response = postMultipart(
                RECEIPT_OCR_PATH,
                multipartBody(originalFilename, content),
                ReceiptOcrResponse.class
        );
        validateReceiptOcr(response);
        return response;
    }

    public FridgeDetectionResponse detectFridgeObjects(MultipartFile file) {
        FridgeDetectionResponse response = postMultipart(FRIDGE_DETECTION_PATH, file, FridgeDetectionResponse.class);
        validateFridgeDetection(response);
        return response;
    }

    private <T> T postMultipart(String path, MultipartFile file, Class<T> responseType) {
        return postMultipart(path, multipartBody(file), responseType);
    }

    private <T> T postMultipart(String path, MultiValueMap<String, Object> body, Class<T> responseType) {
        try {
            return restClient.post()
                    .uri(path)
                    .headers(headers -> {
                        headers.setBearerAuth(token);
                        headers.set("X-Request-Id", UUID.randomUUID().toString());
                    })
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (ResourceAccessException exception) {
            if (isTimeout(exception)) {
                throw new AiServerException(AiServerErrorCode.AI_SERVER_TIMEOUT, exception);
            }
            throw new AiServerException(AiServerErrorCode.AI_SERVER_UNAVAILABLE, exception);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is5xxServerError()) {
                throw new AiServerException(AiServerErrorCode.AI_SERVER_UNAVAILABLE, exception);
            }
            throw new AiServerException(AiServerErrorCode.AI_RESPONSE_INVALID, exception);
        } catch (RestClientException exception) {
            if (isInvalidJson(exception)) {
                throw new AiServerException(AiServerErrorCode.AI_RESPONSE_INVALID, exception);
            }
            throw new AiServerException(AiServerErrorCode.AI_SERVER_UNAVAILABLE, exception);
        }
    }

    private MultiValueMap<String, Object> multipartBody(MultipartFile file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            body.add("file", file.getResource());
            return body;
        } catch (IllegalStateException exception) {
            throw new BusinessValidationException("file resource must be available", exception);
        }
    }

    private MultiValueMap<String, Object> multipartBody(String originalFilename, byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessValidationException("file must not be empty");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(content, originalFilename));
        return body;
    }

    private static class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    private static RestClient createRestClient(AiServerProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private static boolean isTimeout(Throwable throwable) {
        return hasCause(throwable, SocketTimeoutException.class);
    }

    private static boolean isInvalidJson(Throwable throwable) {
        return hasCause(throwable, JsonProcessingException.class);
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void validateFoodClassification(FoodClassificationResponse response) {
        if (response == null
                || response.bestMatch() == null
                || response.confidence() == null
                || response.top3() == null
                || response.source() == null
                || response.top3().stream().anyMatch(AiServerClient::isInvalidFoodCandidate)) {
            throw new AiServerException(AiServerErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private static boolean isInvalidFoodCandidate(FoodClassificationResponse.FoodCandidate item) {
        return item == null || item.name() == null || item.confidence() == null;
    }

    private static void validateReceiptOcr(ReceiptOcrResponse response) {
        if (response == null
                || response.ingredients() == null) {
            throw new AiServerException(AiServerErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private static void validateFridgeDetection(FridgeDetectionResponse response) {
        if (response == null || response.objects() == null || response.objects().stream().anyMatch(AiServerClient::isInvalidObject)) {
            throw new AiServerException(AiServerErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private static boolean isInvalidObject(FridgeDetectionResponse.DetectedObject object) {
        return object == null
                || object.name() == null
                || object.confidence() == null
                || object.box() == null
                || object.box().x1() == null
                || object.box().y1() == null
                || object.box().x2() == null
                || object.box().y2() == null;
    }
}
