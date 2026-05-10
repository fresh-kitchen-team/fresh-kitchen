package com.example.freshkitchen.application.auth.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTokenResultTest {

    @Test
    void create_holdsAccessToken() {
        AuthTokenResult result = new AuthTokenResult("access-token", "refresh-token", false);
        assertThat(result.accessToken()).isEqualTo("access-token");
    }

    @Test
    void create_holdsRefreshToken() {
        AuthTokenResult result = new AuthTokenResult("access-token", "refresh-token", false);
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void create_holdsNewUserFalse() {
        AuthTokenResult result = new AuthTokenResult("access-token", "refresh-token", false);
        assertThat(result.newUser()).isFalse();
    }

    @Test
    void create_holdsNewUserTrue_whenNewUser() {
        AuthTokenResult result = new AuthTokenResult("access-token", "refresh-token", true);
        assertThat(result.newUser()).isTrue();
    }

    @Test
    void recordEquality_whenAllFieldsMatch() {
        AuthTokenResult a = new AuthTokenResult("acc", "ref", true);
        AuthTokenResult b = new AuthTokenResult("acc", "ref", true);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void recordInequality_whenAccessTokenDiffers() {
        AuthTokenResult a = new AuthTokenResult("acc-1", "ref", false);
        AuthTokenResult b = new AuthTokenResult("acc-2", "ref", false);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void recordInequality_whenRefreshTokenDiffers() {
        AuthTokenResult a = new AuthTokenResult("acc", "ref-1", false);
        AuthTokenResult b = new AuthTokenResult("acc", "ref-2", false);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void recordInequality_whenNewUserDiffers() {
        AuthTokenResult a = new AuthTokenResult("acc", "ref", true);
        AuthTokenResult b = new AuthTokenResult("acc", "ref", false);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCode_isConsistentWithEquality() {
        AuthTokenResult a = new AuthTokenResult("acc", "ref", false);
        AuthTokenResult b = new AuthTokenResult("acc", "ref", false);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toString_containsAllFields() {
        AuthTokenResult result = new AuthTokenResult("acc", "ref", true);
        String str = result.toString();
        assertThat(str).contains("acc").contains("ref").contains("true");
    }

    @Test
    void create_allowsNullAccessToken() {
        AuthTokenResult result = new AuthTokenResult(null, "refresh-token", false);
        assertThat(result.accessToken()).isNull();
    }

    @Test
    void create_allowsNullRefreshToken() {
        AuthTokenResult result = new AuthTokenResult("access-token", null, false);
        assertThat(result.refreshToken()).isNull();
    }
}