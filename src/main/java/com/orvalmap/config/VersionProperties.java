package com.orvalmap.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.version")
@Data
public class VersionProperties {
    private Android android;

    @Data
    public static class Android {
        private String latest;
        private String minimum;
    }
}
