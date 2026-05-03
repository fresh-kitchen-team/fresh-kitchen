package com.example.freshkitchen.global.security.infrastructure;

import com.example.freshkitchen.global.security.Role;
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

    // --- Constructor validation ---

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

    // --- Token generation ---

    @Test
    void generateAccessToken_returnsValidJwt() {
        String token = provider.generateAccessToken(1L, Role.USER);

        assertThat(token).isNotBlank();
        TokenPayload payload = provider.validateAccessToken(token);
        assertThat(payload.userId()).isEqualTo(1L);
        assertThat(payload.role()).isEqualTo(Role.USER);
    }

    @Test
    void generateRefreshToken_returnsValidJwt() {
        String token = provider.generateRefreshToken(1L);

        assertThat(token).isNotBlank();
        Long userId = provider.validateRefreshToken(token);
        assertThat(userId).isEqualTo(1L);
    }

    @Test
    void generateAccessToken_containsRoleAndTokenTypeClaims() {
        String token = provider.generateAccessToken(1L, Role.USER);

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
        String token = provider.generateAccessToken(1L, Role.USER);

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
        assertThatThrownBy(() -> provider.generateAccessToken(null, Role.USER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateAccessToken_throwsException_whenRoleIsNull() {
        assertThatThrownBy(() -> provider.generateAccessToken(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateRefreshToken_throwsException_whenUserIdIsNull() {
        assertThatThrownBy(() -> provider.generateRefreshToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- validateAccessToken: happy path ---

    @Test
    void validateAccessToken_returnsPayloadWithUserIdAndRole() {
        Long userId = 42L;
        String token = provider.generateAccessToken(userId, Role.USER);

        TokenPayload payload = provider.validateAccessToken(token);

        assertThat(payload.userId()).isEqualTo(userId);
        assertThat(payload.role()).isEqualTo(Role.USER);
    }

    // --- validateRefreshToken: happy path ---

    @Test
    void validateRefreshToken_returnsUserId() {
        Long userId = 99L;
        String token = provider.generateRefreshToken(userId);

        Long extracted = provider.validateRefreshToken(token);

        assertThat(extracted).isEqualTo(userId);
    }

    // --- Token type enforcement (#1) ---

    @Test
    void validateAccessToken_throwsMalformed_whenRefreshTokenIsPresented() {
        String refreshToken = provider.generateRefreshToken(1L);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(refreshToken)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    @Test
    void validateRefreshToken_throwsMalformed_whenAccessTokenIsPresented() {
        String accessToken = provider.generateAccessToken(1L, Role.USER);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateRefreshToken(accessToken)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    // --- Signature & expiration errors ---

    @Test
    void validateAccessToken_throwsExpired_whenTokenAlreadyExpired() {
        String expiredToken = buildExpiredAccessToken(1L);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(expiredToken)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.EXPIRED_TOKEN);
    }

    @Test
    void validateAccessToken_throwsInvalidSignature_whenTokenSignedWithDifferentSecret() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(OTHER_SECRET, ACCESS_EXP_MIN, REFRESH_EXP_DAYS);
        String tampered = otherProvider.generateAccessToken(1L, Role.USER);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(tampered)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.INVALID_SIGNATURE);
    }

    @Test
    void validateAccessToken_throwsMalformed_whenTokenIsArbitraryString() {
        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken("this-is-not-a-valid-jwt")
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    @Test
    void validateAccessToken_throwsUnsupported_whenTokenIsUnsigned() {
        String unsigned = Jwts.builder().subject("1").compact();

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(unsigned)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.UNSUPPORTED_TOKEN);
    }

    @Test
    void validateAccessToken_throwsEmptyClaims_whenTokenIsBlank() {
        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken("")
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.EMPTY_CLAIMS);
    }

    @Test
    void validateAccessToken_throwsEmptyClaims_whenTokenIsNull() {
        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(null)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.EMPTY_CLAIMS);
    }

    @Test
    void validateRefreshToken_throwsEmptyClaims_whenTokenIsNull() {
        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateRefreshToken(null)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.EMPTY_CLAIMS);
    }

    @Test
    void validateAccessToken_throwsNotYetValid_whenTokenIsPremature() {
        String token = buildPrematureAccessToken(1L);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.NOT_YET_VALID_TOKEN);
    }

    // --- Claim validation: userId ---

    @Test
    void validateAccessToken_throwsMalformed_whenUserIdClaimIsMissing() {
        String token = buildAccessTokenWithoutUserId();

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    // --- Claim validation: expiration ---

    @Test
    void validateAccessToken_throwsMalformed_whenExpirationClaimIsMissing() {
        String token = buildAccessTokenWithoutExpiration(1L);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    // --- Claim validation: tokenType ---

    @Test
    void validateAccessToken_throwsMalformed_whenTokenTypeClaimIsMissing() {
        String token = buildTokenWithoutTokenType(1L);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    @Test
    void validateAccessToken_throwsMalformed_whenTokenTypeIsUnknownValue() {
        String token = buildTokenWithTokenType(1L, "guest");

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    @Test
    void validateAccessToken_throwsMalformed_whenTokenTypeIsNotString() {
        String token = buildTokenWithNonStringTokenType(1L);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    // --- Claim validation: role ---

    @Test
    void validateAccessToken_throwsMalformed_whenRoleClaimIsMissing() {
        String token = buildAccessTokenWithoutRole(1L);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    @Test
    void validateAccessToken_throwsMalformed_whenRoleClaimIsBlank() {
        String token = buildAccessTokenWithRole(1L, "  ");

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    @Test
    void validateAccessToken_throwsMalformed_whenRoleIsNotInEnum() {
        String token = buildAccessTokenWithRole(1L, "ADMIN");

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    @Test
    void validateAccessToken_throwsMalformed_whenRoleIsLowercaseEnumName() {
        String token = buildAccessTokenWithRole(1L, "user");

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.MALFORMED_TOKEN);
    }

    @Test
    void validateRefreshToken_doesNotRequireRoleClaim() {
        String token = provider.generateRefreshToken(1L);

        Long userId = provider.validateRefreshToken(token);

        assertThat(userId).isEqualTo(1L);
    }

    // --- Clock skew tolerance (#4) ---

    @Test
    void validateAccessToken_toleratesClockSkewWithin30Seconds() {
        String token = buildAccessTokenExpiringSecondsAgo(25);

        TokenPayload payload = provider.validateAccessToken(token);

        assertThat(payload.userId()).isEqualTo(1L);
    }

    @Test
    void validateAccessToken_throwsExpired_whenBeyondClockSkewTolerance() {
        String token = buildAccessTokenExpiringSecondsAgo(35);

        JwtTokenException exception = catchThrowableOfType(
                JwtTokenException.class,
                () -> provider.validateAccessToken(token)
        );

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(JwtErrorCode.EXPIRED_TOKEN);
    }

    // --- Helper methods ---

    private String buildExpiredAccessToken(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date issuedAt = new Date(System.currentTimeMillis() - 120_000);
        Date expiration = new Date(System.currentTimeMillis() - 60_000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", Role.USER.name())
                .claim("tokenType", "access")
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String buildAccessTokenExpiringSecondsAgo(int secondsAgo) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now - 60_000);
        Date expiration = new Date(now - (secondsAgo * 1000L));
        return Jwts.builder()
                .subject("1")
                .claim("userId", 1L)
                .claim("role", Role.USER.name())
                .claim("tokenType", "access")
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String buildPrematureAccessToken(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date notBefore = new Date(now.getTime() + 60_000);
        Date expiration = new Date(now.getTime() + 120_000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", Role.USER.name())
                .claim("tokenType", "access")
                .issuedAt(now)
                .notBefore(notBefore)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String buildAccessTokenWithoutUserId() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date issuedAt = new Date();
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        return Jwts.builder()
                .subject("1")
                .claim("role", Role.USER.name())
                .claim("tokenType", "access")
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String buildAccessTokenWithoutExpiration(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", Role.USER.name())
                .claim("tokenType", "access")
                .issuedAt(new Date())
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
                .claim("role", Role.USER.name())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String buildTokenWithTokenType(Long userId, String tokenType) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date issuedAt = new Date();
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", Role.USER.name())
                .claim("tokenType", tokenType)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String buildTokenWithNonStringTokenType(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date issuedAt = new Date();
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", Role.USER.name())
                .claim("tokenType", 1)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String buildAccessTokenWithoutRole(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date issuedAt = new Date();
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("tokenType", "access")
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String buildAccessTokenWithRole(Long userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date issuedAt = new Date();
        Date expiration = new Date(System.currentTimeMillis() + 60_000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", role)
                .claim("tokenType", "access")
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
