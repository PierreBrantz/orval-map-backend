package com.orvalmap.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    private final RestClient brevoClient;
    private final String senderEmail;
    private final String senderName;

    public EmailService(
            RestClient.Builder restClientBuilder,
            @Value("${BREVO_API_KEY}") String brevoApiKey,
            @Value("${SENDER_EMAIL}") String senderEmail,
            @Value("${SENDER_NAME:Orval Maps}") String senderName) {
        this.brevoClient = restClientBuilder
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", brevoApiKey)
                .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    public void sendEmail(String to, String subject, String body) {
        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", body
        );

        try {
            var response = brevoClient.post()
                    .uri("/smtp/email")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("E-mail envoyé à {} via Brevo, statut: {}", to, response.getStatusCode());
        } catch (RestClientResponseException ex) {
            log.error(
                    "Brevo a refusé l'e-mail destiné à {} (statut {}): {}",
                    to,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString(),
                    ex
            );
        } catch (RestClientException ex) {
            log.error("Erreur lors de l'envoi de l'e-mail à {} via Brevo", to, ex);
        }
    }
}
