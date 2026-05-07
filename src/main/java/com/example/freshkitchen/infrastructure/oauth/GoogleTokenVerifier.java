package com.example.freshkitchen.infrastructure.oauth;

import com.example.freshkitchen.global.security.exception.OAuthErrorCode;
import com.example.freshkitchen.global.security.exception.OAuthException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Component
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${oauth.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(List.of(clientId))
                .build();
    }

    public GoogleUserInfo verify(String idTokenString) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException e) {
            throw new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN, e);
        }

        if (idToken == null) {
            throw new OAuthException(OAuthErrorCode.INVALID_ID_TOKEN);
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        return new GoogleUserInfo(payload.getSubject(), payload.getEmail());
    }

    public record GoogleUserInfo(String sub, String email) {
    }
}
