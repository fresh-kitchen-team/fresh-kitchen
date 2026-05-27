package com.example.freshkitchen.domain.alarm.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@Slf4j
public class FCMInitializer {

    @Value("${fcm.firebase-credentials-location}")
    private Resource credentialsResource;

    @PostConstruct
    public void initialize() {
        if (credentialsResource == null || !credentialsResource.exists()) {
            log.warn("FCM credentials not found at {}, skipping Firebase initialization",
                    credentialsResource != null ? credentialsResource.getDescription() : "(null)");
            return;
        }

        try (InputStream in = credentialsResource.getInputStream()) {
            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(in);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(googleCredentials)
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase application has been initialized from {}", credentialsResource.getDescription());
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase", e);
        }
    }
}
