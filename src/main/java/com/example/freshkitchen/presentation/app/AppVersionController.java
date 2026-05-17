package com.example.freshkitchen.presentation.app;

import com.example.freshkitchen.global.response.ApiResponse;
import com.example.freshkitchen.infrastructure.app.AppVersionProperties;
import io.swagger.v3.oas.annotations.media.Schema;
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
            @Schema(description = "최신 앱 버전", example = "1.0.0")
            String latestVersion,

            @Schema(description = "필수 최소 버전 (이 버전 미만이면 강제 업데이트 필요)", example = "1.0.0")
            String minimumVersion,

            @Schema(description = "강제 업데이트 여부", example = "false")
            boolean forceUpdate,

            @Schema(description = "앱 업데이트 링크 (플레이스토어 등)", example = "https://play.google.com/store/apps/details?id=com.freshkitchen")
            String updateUrl
    ) {
    }
}
