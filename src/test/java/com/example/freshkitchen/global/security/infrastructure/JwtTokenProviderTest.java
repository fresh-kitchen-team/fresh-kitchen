package com.example.freshkitchen.global.security.infrastructure;

import com.example.freshkitchen.global.security.exception.CustomJwtException;
import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long-for-hmac-sha256";
    private static final String OTHER_SECRET = "another-secret-key-must-be-at-least-32-bytes-long-for-hmac";
    private static final long ACCESS_EXP_MIN = 30L;
    private static final long REFRESH_EXP_DAYS = 14L;

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, ACCESS_EXP_MIN, REFRESH_EXP_DAYS);
    }

    @Test
    void generateAccessToken_returnsValidJwt() {
        String token = provider.generateAccessToken(1L, "USER");

        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void generateRefreshToken_returnsValidJwt() {
        String token = provider.generateRefreshToken(1L);

        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void getUserIdFromToken_returnsSameUserIdAsIssued() {
        Long userId = 42L;
        String token = provider.generateAccessToken(userId, "USER");

        Long extracted = provider.getUserIdFromToken(token);

        assertThat(extracted).isEqualTo(userId);
    }

    @Test
    void getUserIdFromToken_worksForRefreshToken() {
        Long userId = 99L;
        String token = provider.generateRefreshToken(userId);

        Long extracted = provider.getUserIdFromToken(token);

        assertThat(extracted).isEqualTo(userId);
    }

    @Test
    void validateToken_throwsExpired_whenTokenAlreadyExpired() throws InterruptedException {
        JwtTokenProvider zeroExpProvider = new JwtTokenProvider(SECRET, 0L, 0L);
        String token = zeroExpProvider.generateAccessToken(1L, "USER");
        Thread.sleep(10);

        CustomJwtException exception = catchThrowableOfType(
                CustomJwtException.class,
                () -> zeroExpProvider.validateToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.EXPIRED_TOKEN);
    }

    @Test
    void validateToken_throwsInvalidSignature_whenTokenSignedWithDifferentSecret() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(OTHER_SECRET, ACCESS_EXP_MIN, REFRESH_EXP_DAYS);
        String tampered = otherProvider.generateAccessToken(1L, "USER");

        CustomJwtException exception = catchThrowableOfType(
                CustomJwtException.class,
                () -> provider.validateToken(tampered)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.INVALID_SIGNATURE);
    }

    @Test
    void validateToken_throwsMalformed_whenTokenIsArbitraryString() {
        String malformed = "this-is-not-a-valid-jwt";

        CustomJwtException exception = catchThrowableOfType(
                CustomJwtException.class,
                () -> provider.validateToken(malformed)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    @Test
    void validateToken_throwsEmptyClaims_whenTokenIsBlank() {
        CustomJwtException exception = catchThrowableOfType(
                CustomJwtException.class,
                () -> provider.validateToken("")
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.EMPTY_CLAIMS);
    }

    @Test
    void getUserIdFromToken_throwsExpired_whenAccessingExpiredToken() throws InterruptedException {
        JwtTokenProvider zeroExpProvider = new JwtTokenProvider(SECRET, 0L, 0L);
        String token = zeroExpProvider.generateAccessToken(1L, "USER");
        Thread.sleep(10);

        CustomJwtException exception = catchThrowableOfType(
                CustomJwtException.class,
                () -> zeroExpProvider.getUserIdFromToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.EXPIRED_TOKEN);
    }
}
