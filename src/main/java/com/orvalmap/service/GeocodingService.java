package com.orvalmap.service;

import com.orvalmap.model.GeocodingResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Optional;

@Service
@Slf4j
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    public Optional<GeocodingResultDTO> getCoordinates(String placeName, String city) {
        String query = placeName + ", " + city;

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(NOMINATIM_URL)
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("limit", 1);

        try {
            GeocodingResultDTO[] results = restTemplate.getForObject(builder.toUriString(), GeocodingResultDTO[].class);
            if (results != null && results.length > 0) {
                log.info("Coordonnées trouvées pour '{}': lat={}, lon={}", query, results[0].getLat(), results[0].getLon());
                return Optional.of(results[0]);
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à l'API de géocodage pour '{}'", query, e);
        }

        log.warn("Aucune coordonnée trouvée pour '{}'", query);
        return Optional.empty();
    }
}
