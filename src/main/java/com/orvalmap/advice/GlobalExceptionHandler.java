package com.orvalmap.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException ex) {
        // On vérifie si le message est celui de la distance pour être sûr
        if (ex.getMessage().contains("Vous n'êtes pas assez proche")) {
            Map<String, String> errorBody = Map.of("error", ex.getMessage());
            return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);
        }
        
        // Pour les autres IllegalStateException, on peut garder une erreur 500
        Map<String, String> errorBody = Map.of("error", "An unexpected error occurred");
        return new ResponseEntity<>(errorBody, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
