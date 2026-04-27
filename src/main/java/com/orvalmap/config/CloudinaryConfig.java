package com.orvalmap.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryConfig.class);

    @Value("${cloudinary.cloud_name:votre_cloud_name}")
    private String cloudName;

    @Value("${cloudinary.api_key:votre_api_key}")
    private String apiKey;

    @Value("${cloudinary.api_secret:votre_api_secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        logger.info("Initializing Cloudinary with cloud_name: {}", cloudName);
        
        if ("root".equals(cloudName)) {
            logger.error("CRITICAL: Cloudinary cloud_name is set to 'root'. This will cause errors.");
        }

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        return new Cloudinary(config);
    }
}
