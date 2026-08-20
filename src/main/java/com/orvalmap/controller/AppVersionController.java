package com.orvalmap.controller;

import com.orvalmap.dto.AppVersionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
public class AppVersionController {

    private final String latestVersion;
    private final String minimumVersion;
    private final String playStoreUrl;

    public AppVersionController(
            @Value("${app.version.android.latest}") String latestVersion,
            @Value("${app.version.android.minimum}") String minimumVersion,
            @Value("${app.version.android.store-url}") String playStoreUrl
    ) {
        this.latestVersion = latestVersion;
        this.minimumVersion = minimumVersion;
        this.playStoreUrl = playStoreUrl;
    }

    @GetMapping("/version")
    public AppVersionResponse getAppVersion() {
        return new AppVersionResponse(
                latestVersion,
                minimumVersion,
                playStoreUrl
        );
    }
}
