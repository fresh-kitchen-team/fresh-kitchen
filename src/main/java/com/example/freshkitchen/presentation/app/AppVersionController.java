package com.example.freshkitchen.presentation.app;

import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.infrastructure.app.AppVersionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app")
@RequiredArgsConstructor
public class AppVersionController {

    private final AppVersionProperties appVersionProperties;

    @GetMapping("/version")
    public ResponseEntity<ApiResponse<VersionResponse>> version() {
        return ApiResponse.success(new VersionResponse(
                appVersionProperties.getLatestVersion(),
                appVersionProperties.getMinimumVersion(),
                appVersionProperties.isForceUpdate(),
                appVersionProperties.getUpdateUrl()
        ));
    }

    public record VersionResponse(
            String latestVersion,
            String minimumVersion,
            boolean forceUpdate,
            String updateUrl
    ) {
    }
}
