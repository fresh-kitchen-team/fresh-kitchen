package com.example.freshkitchen.global.security.infrastructure;

import com.example.freshkitchen.global.security.exception.JwtErrorCode;
import com.example.freshkitchen.global.security.exception.JwtTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void constructor_throwsException_whenSecretIsBlank() {
        assertThatThrownBy(() -> new JwtTokenProvider("", ACCESS_EXP_MIN, REFRESH_EXP_DAYS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_throwsException_whenSecretIsTooShort() {
        assertThatThrownBy(() -> new JwtTokenProvider("short", ACCESS_EXP_MIN, REFRESH_EXP_DAYS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_throwsException_whenAccessExpirationIsNotPositive() {
        assertThatThrownBy(() -> new JwtTokenProvider(SECRET, 0L, REFRESH_EXP_DAYS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_throwsException_whenRefreshExpirationIsNotPositive() {
        assertThatThrownBy(() -> new JwtTokenProvider(SECRET, ACCESS_EXP_MIN, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateAccessToken_returnsValidJwt() {
        String token = provider.generateAccessToken(1L, "USER");

        assertThat(token).isNotBlank();
        assertThatCode(() -> provider.validateToken(token)).doesNotThrowAnyException();
    }

    @Test
    void generateRefreshToken_returnsValidJwt() {
        String token = provider.generateRefreshToken(1L);

        assertThat(token).isNotBlank();
        assertThatCode(() -> provider.validateToken(token)).doesNotThrowAnyException();
    }

    @Test
    void generateAccessToken_containsRoleAndTokenTypeClaims() {
        String token = provider.generateAccessToken(1L, "USER");

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.get("role")).isEqualTo("USER");
        assertThat(claims.get("tokenType")).isEqualTo("access");
        assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
    }

    @Test
    void generateRefreshToken_containsTokenTypeClaimWithoutRole() {
        String token = provider.generateRefreshToken(1L);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.get("tokenType")).isEqualTo("refresh");
        assertThat(claims.get("role")).isNull();
        assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
    }

    @Test
    void generateAccessToken_setsExpirationFromAccessExpirationMinutes() {
        String token = provider.generateAccessToken(1L, "USER");

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        long ttlMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(ttlMillis).isEqualTo(Duration.ofMinutes(ACCESS_EXP_MIN).toMillis());
    }

    @Test
    void generateRefreshToken_setsExpirationFromRefreshExpirationDays() {
        String token = provider.generateRefreshToken(1L);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        long ttlMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(ttlMillis).isEqualTo(Duration.ofDays(REFRESH_EXP_DAYS).toMillis());
    }

    @Test
    void generateAccessToken_throwsException_whenUserIdIsNull() {
        assertThatThrownBy(() -> provider.generateAccessToken(null, "USER"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateAccessToken_throwsException_whenRoleIsNull() {
        assertThatThrownBy(() -> provider.generateAccessToken(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateAccessToken_throwsException_whenRoleIsBlank() {
        assertThatThrownBy(() -> provider.generateAccessToken(1L, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateRefreshToken_throwsException_whenUserIdIsNull() {
        assertThatThrownBy(() -> provider.generateRefreshToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateAndGetUserId_returnsSameUserIdAsIssued() {
        Long userId = 42L;
        String token = provider.generateAccessToken(userId, "USER");

        Long extracted = provider.validateAndGetUserId(token);

        assertThat(extracted).isEqualTo(userId);
    }

    @Test
    void validateAndGetUserId_worksForRefreshToken() {
        Long userId = 99L;
        String token = provider.generateRefreshToken(userId);

        Long extracted = provider.validateAndGetUserId(token);

        assertThat(extracted).isEqualTo(userId);
    }

    @Test
    void validateToken_throwsExpired_whenTokenAlreadyExpired() {
        String expiredToken = buildExpiredToken(1L);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateToken(expiredToken)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.EXPIRED_TOKEN);
    }

    @Test
    void validateToken_throwsInvalidSignature_whenTokenSignedWithDifferentSecret() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(OTHER_SECRET, ACCESS_EXP_MIN, REFRESH_EXP_DAYS);
        String tampered = otherProvider.generateAccessToken(1L, "USER");

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateToken(tampered)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.INVALID_SIGNATURE);
    }

    @Test
    void validateToken_throwsMalformed_whenTokenIsArbitraryString() {
        String malformed = "this-is-not-a-valid-jwt";

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateToken(malformed)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    @Test
    void validateToken_throwsUnsupported_whenTokenIsUnsigned() {
        String unsigned = Jwts.builder()
                .subject("1")
                .compact();

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateToken(unsigned)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.UNSUPPORTED_TOKEN);
    }

    @Test
    void validateToken_throwsEmptyClaims_whenTokenIsBlank() {
        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateToken("")
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.EMPTY_CLAIMS);
    }

    @Test
    void validateAndGetUserId_throwsExpired_whenAccessingExpiredToken() {
        String expiredToken = buildExpiredToken(1L);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAndGetUserId(expiredToken)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.EXPIRED_TOKEN);
    }

    @Test
    void validateToken_throwsMalformed_whenUserIdClaimIsMissing() {
        String token = buildTokenWithoutUserId();

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    @Test
    void validateToken_throwsMalformed_whenExpirationClaimIsMissing() {
        String token = buildTokenWithoutExpiration(1L);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    @Test
    void validateToken_throwsExpired_whenTokenIsPremature() {
        String token = buildPrematureToken(1L);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.EXPIRED_TOKEN);
    }

    @Test
    void validateToken_throwsMalformed_whenTokenTypeClaimIsMissing() {
        String token = buildTokenWithoutTokenType(1L);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    private String buildExpiredToken(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date issuedAt = new Date(System.currentTimeMillis() - 120_000);
        Date expiration = new Date(System.currentTimeMillis() - 60_000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", "USER")
                .claim("tokenType", "access")
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String buildTokenWithoutUserId() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date issuedAt = new Date();
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        return Jwts.builder()
                .subject("1")
                .claim("role", "USER")
                .claim("tokenType", "access")
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String buildTokenWithoutExpiration(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", "USER")
                .claim("tokenType", "access")
                .issuedAt(new Date())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String buildPrematureToken(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date notBefore = new Date(now.getTime() + 60_000);
        Date expiration = new Date(now.getTime() + 120_000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", "USER")
                .claim("tokenType", "access")
                .issuedAt(now)
                .notBefore(notBefore)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String buildTokenWithoutTokenType(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date issuedAt = new Date();
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", "USER")
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
