package com.orvalmap.controller;

import com.orvalmap.model.Place;
import com.orvalmap.model.PlaceRequest;
import com.orvalmap.service.PlaceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/place-requests")
@RequiredArgsConstructor
public class PlaceRequestController {

    private final PlaceRequestService placeRequestService;

    // 🔒 Réservé aux utilisateurs connectés
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<PlaceRequest> submitRequest(@Valid @RequestBody PlaceRequest request, Authentication authentication) {
        return ResponseEntity.ok(placeRequestService.createRequest(request, authentication.getName()));
    }

    // ✅ Upload d'image pour une suggestion (avant création)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = placeRequestService.uploadRequestImage(file);
            return ResponseEntity.ok(Map.of("url", imageUrl));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erreur lors de l'upload de l'image : " + e.getMessage());
        }
    }

    // 🔒 Réservé aux ADMIN
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    @GetMapping("/pending")
    public List<PlaceRequest> getPendingRequests() {
        return placeRequestService.getAllPendingRequests();
    }

    // 🔒 Réservé aux ADMIN
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    @PostMapping("/{id}/validate")
    public ResponseEntity<Place> validateRequest(@PathVariable Long id) {
        return ResponseEntity.ok(placeRequestService.validateRequest(id));
    }

    // 🔒 Réservé aux ADMIN
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectRequest(@PathVariable Long id) {
        placeRequestService.rejectRequest(id);
        return ResponseEntity.noContent().build();
    }
}
