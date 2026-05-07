package com.example.freshkitchen.infrastructure.oauth;

import com.example.freshkitchen.global.security.exception.OAuthErrorCode;
import com.example.freshkitchen.global.security.exception.OAuthException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KakaoTokenVerifier {

    private static final String JWKS_URI = "https://kauth.kakao.com/.well-known/jwks.json";
    private static final String ISSUER = "https://kauth.kakao.com";

    private final String clientId;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Map<String, RSAPublicKey> keyCache = new ConcurrentHashMap<>();

    public KakaoTokenVerifier(
            @Value("${oauth.kakao.client-id}") String clientId,
            ObjectMapper objectMapper
    ) {
        this.clientId = clientId;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public KakaoUserInfo verify(String idTokenString) {
        try {
            String kid = extractKid(idTokenString);
            RSAPublicKey publicKey = getPublicKey(kid);

            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(ISSUER)
                    .requireAudience(clientId)
                    .build()
                    .parseSignedClaims(idTokenString)
                    .getPayload();

            return new KakaoUserInfo(claims.getSubject(), claims.get("email", String.class));
        } catch (OAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN, e);
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

        refreshKeys();

        RSAPublicKey key = keyCache.get(kid);
        if (key == null) {
            throw new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN);
        }
        return key;
    }

    private void refreshKeys() {
        try {
            String jwksJson = restClient.get()
                    .uri(JWKS_URI)
                    .retrieve()
                    .body(String.class);

            JsonNode keys = objectMapper.readTree(jwksJson).get("keys");
            for (JsonNode keyNode : keys) {
                String kid = keyNode.get("kid").asText();
                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(keyNode.get("n").asText()));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(keyNode.get("e").asText()));

                RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                        .generatePublic(new RSAPublicKeySpec(modulus, exponent));
                keyCache.put(kid, publicKey);
            }
        } catch (Exception e) {
            throw new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN, e);
        }
    }

    public record KakaoUserInfo(String sub, String email) {
    }
}
