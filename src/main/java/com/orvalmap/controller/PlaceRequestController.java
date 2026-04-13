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

import java.util.List;

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

    // 🔒 Réservé aux ADMIN
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    @GetMapping("/pending") // ✅ Changé de @GetMapping à @GetMapping("/pending")
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
