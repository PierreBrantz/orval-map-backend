package com.orvalmap.advice;

import com.orvalmap.exception.DuplicatePlaceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsReadableErrorForOversizedPhoto() {
        ResponseEntity<Map<String, String>> response =
                handler.handleUploadTooLarge(new MaxUploadSizeExceededException(10 * 1024 * 1024));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).containsEntry("error",
                "La photo est trop volumineuse. Compressez-la ou choisissez une photo plus petite.");
    }

    @Test
    void returnsReadableConflictForDuplicatePlace() {
        DuplicatePlaceException exception = new DuplicatePlaceException(
                "Ce bar existe déjà sur la carte : « Le Porthuis ».",
                "PLACE",
                42L
        );

        ResponseEntity<Map<String, Object>> response = handler.handleDuplicatePlace(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry(
                "error", "Ce bar existe déjà sur la carte : « Le Porthuis ».");
        assertThat(response.getBody()).containsEntry("duplicateType", "PLACE");
        assertThat(response.getBody()).containsEntry("duplicateId", 42L);
    }
}
