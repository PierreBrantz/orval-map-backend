package com.orvalmap.controller;

import com.orvalmap.config.VersionProperties;
import com.orvalmap.model.VersionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/version")
@RequiredArgsConstructor
public class VersionController {

    private final VersionProperties versionProperties;

    @GetMapping
    public ResponseEntity<VersionDTO> getVersionInfo() {
        VersionDTO.AndroidVersion androidVersion = new VersionDTO.AndroidVersion();
        androidVersion.setLatestVersion(versionProperties.getAndroid().getLatest());
        androidVersion.setMinimumVersion(versionProperties.getAndroid().getMinimum());

        VersionDTO versionDTO = new VersionDTO();
        versionDTO.setAndroid(androidVersion);

        return ResponseEntity.ok(versionDTO);
    }
}
