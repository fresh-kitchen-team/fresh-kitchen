package com.example.freshkitchen.global.response;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiResponseTest {

    @Test
    void onSuccess_usesOkByDefault() {
        ApiResponse<String> response = ApiResponse.onSuccess("data");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.code()).isEqualTo("COMMON-200");
        assertThat(response.message()).isEqualTo("Success");
        assertThat(response.data()).isEqualTo("data");
    }

    @Test
    void onSuccess_usesGivenSuccessStatus() {
        ApiResponse<String> response = ApiResponse.onSuccess(HttpStatus.CREATED, "data");

        assertThat(response.status()).isEqualTo(201);
        assertThat(response.code()).isEqualTo("COMMON-201");
        assertThat(response.message()).isEqualTo("Success");
        assertThat(response.data()).isEqualTo("data");
    }

    @Test
    void success_returnsResponseEntityWithMatchingStatus() {
        ResponseEntity<ApiResponse<String>> response = ApiResponse.success(HttpStatus.CREATED, "data");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(201);
        assertThat(response.getBody().code()).isEqualTo("COMMON-201");
        assertThat(response.getBody().message()).isEqualTo("Success");
        assertThat(response.getBody().data()).isEqualTo("data");
    }

    @Test
    void success_usesOkWithNullDataByDefault() {
        ResponseEntity<ApiResponse<Void>> response = ApiResponse.success();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(200);
        assertThat(response.getBody().code()).isEqualTo("COMMON-200");
        assertThat(response.getBody().message()).isEqualTo("Success");
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void onSuccess_rejectsNonSuccessStatus() {
        assertThatThrownBy(() -> ApiResponse.onSuccess(HttpStatus.BAD_REQUEST, "data"))
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(IllegalStateException.class)
                .hasMessage("success status must be 2xx");
    }

    @Test
    void onSuccess_rejectsBodylessSuccessStatus() {
        assertThatThrownBy(() -> ApiResponse.onSuccess(HttpStatus.NO_CONTENT, "data"))
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(IllegalStateException.class)
                .hasMessage("success status must allow response body");

        assertThatThrownBy(() -> ApiResponse.onSuccess(HttpStatus.RESET_CONTENT, "data"))
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(IllegalStateException.class)
                .hasMessage("success status must allow response body");
    }

    @Test
    void success_rejectsNullStatusBeforeCreatingResponseEntity() {
        assertThatThrownBy(() -> ApiResponse.success(null, "data"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("status must not be null");
    }
}
