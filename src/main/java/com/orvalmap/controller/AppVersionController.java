package com.orvalmap.controller;

import com.orvalmap.dto.AppVersionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
public class AppVersionController {

    @GetMapping("/version")
    public AppVersionResponse getAppVersion() {
        return new AppVersionResponse(
                "1.1.0",
                "1.1.0",
                "https://play.google.com/store/apps/details?id=com.orvalmaps" // Remplacez par votre package name
        );
    }
}
