package com.example.freshkitchen.infrastructure.oauth;

import com.example.freshkitchen.global.security.exception.OAuthErrorCode;
import com.example.freshkitchen.global.security.exception.OAuthException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class KakaoTokenVerifier {

    private static final String JWKS_URI = "https://kauth.kakao.com/.well-known/jwks.json";
    private static final String ISSUER = "https://kauth.kakao.com";
    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofMinutes(5);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final List<String> allowedAudiences;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Object refreshLock = new Object();
    private volatile Map<String, RSAPublicKey> keyCache = Map.of();
    private volatile Instant lastRefreshed = Instant.EPOCH;

    public KakaoTokenVerifier(
            @Value("${oauth.kakao.client-id}") String clientId,
            @Value("${oauth.kakao.app-key:}") String appKey,
            ObjectMapper objectMapper
    ) {
        this.allowedAudiences = appKey.isBlank() ? List.of(clientId) : List.of(clientId, appKey);
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    // nonce 검증 생략: 서버-투-서버 flow에서는 클라이언트가 nonce를 생성하지 않음
    public KakaoUserInfo verify(String idTokenString) {
        try {
            String kid = extractKid(idTokenString);
            RSAPublicKey publicKey = getPublicKey(kid);

            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(idTokenString)
                    .getPayload();
            
            verifyAudience(claims);

            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN);
            }

            return new KakaoUserInfo(subject, claims.get("email", String.class));
        } catch (OAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN, e);
        }
    }

    private void verifyAudience(Claims claims) {
        Set<String> audiences = claims.getAudience();
        if (audiences == null || audiences.stream().noneMatch(allowedAudiences::contains)) {
            throw new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN);
        }
    }

    private String extractKid(String idTokenString) {
        try {
            String header = idTokenString.split("\\.")[0];
            byte[] decoded = Base64.getUrlDecoder().decode(header);
            JsonNode headerNode = objectMapper.readTree(decoded);
            return headerNode.get("kid").asText();
        } catch (Exception e) {
            throw new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN, e);
        }
    }

    private RSAPublicKey getPublicKey(String kid) {
        RSAPublicKey cached = keyCache.get(kid);
        if (cached != null) {
            return cached;
        }

        synchronized (refreshLock) {
            cached = keyCache.get(kid);
            if (cached != null) {
                return cached;
            }

            if (Duration.between(lastRefreshed, Instant.now()).compareTo(MIN_REFRESH_INTERVAL) < 0 && !keyCache.isEmpty()) {
                throw new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN);
            }

            refreshKeys();

            RSAPublicKey key = keyCache.get(kid);
            if (key == null) {
                throw new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN);
            }
            return key;
        }
    }

    private void refreshKeys() {
        try {
            String jwksJson = restClient.get()
                    .uri(JWKS_URI)
                    .retrieve()
                    .body(String.class);

            JsonNode keys = objectMapper.readTree(jwksJson).get("keys");
            Map<String, RSAPublicKey> newKeys = new HashMap<>();
            for (JsonNode keyNode : keys) {
                String kid = keyNode.get("kid").asText();
                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(keyNode.get("n").asText()));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(keyNode.get("e").asText()));

                RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                        .generatePublic(new RSAPublicKeySpec(modulus, exponent));
                newKeys.put(kid, publicKey);
            }
            keyCache = Map.copyOf(newKeys);
            lastRefreshed = Instant.now();
        } catch (Exception e) {
            log.error("Kakao JWKS 갱신 실패", e);
            throw new OAuthException(OAuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE, e);
        }
    }

    public record KakaoUserInfo(String sub, String email) {
    }
}
