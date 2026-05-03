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

    @Value("${CLOUDINARY_CLOUD_NAME:your-default-cloud-name}") // Valeur par défaut pour éviter le crash
    private String cloudName;

    @Value("${CLOUDINARY_API_KEY:your-default-api-key}") // Valeur par défaut pour éviter le crash
    private String apiKey;

    @Value("${CLOUDINARY_API_SECRET:your-default-api-secret}") // Valeur par défaut pour éviter le crash
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        logger.info("Initializing Cloudinary with app.cloudinary.name: {}", cloudName);
        
        if ("root".equals(cloudName)) {
            logger.error("CRITICAL: Cloudinary name is STILL 'root'. Check Railway variables for CLOUDINARY_CLOUD_NAME.");
        }

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        return new Cloudinary(config);
    }
}
