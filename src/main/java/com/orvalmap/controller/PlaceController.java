package com.orvalmap.controller;

import com.orvalmap.model.Place;
import com.orvalmap.service.PlaceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    // ✅ Accessible sans JWT
    // Exemple d'appel : GET /api/places?page=0&size=10&sort=name,asc
    @GetMapping
    public Page<Place> getAllPlaces(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @PageableDefault(size = 20, sort = "name") Pageable pageable // Par défaut : 20 éléments, triés par nom
    ) {
        return placeService.getAllPlaces(city, lng, lat, radius, pageable);
    }

    // ✅ Accessible sans JWT
    @GetMapping("/{id}")
    public ResponseEntity<Place> getPlaceById(@PathVariable Long id) {
        Place place = placeService.getPlaceById(id);
        return (place != null) ? ResponseEntity.ok(place) : ResponseEntity.notFound().build();
    }

    // 🔒 Création réservée aux ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Place addPlace(@Valid @RequestBody Place place) {
        return placeService.addPlace(place);
    }

    // 🔒 Suppression réservée aux ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlace(@PathVariable Long id) {
        placeService.deletePlace(id);
        return ResponseEntity.noContent().build();
    }

    // 🔒 Mise à jour réservée aux ADMIN ou OWNER
    @PreAuthorize("hasRole('ADMIN') or (hasRole('OWNER') and @placeService.isOwner(#id, authentication.name))")
    @PutMapping("/{id}")
    public ResponseEntity<Place> updatePlace(
            @PathVariable Long id,
            @Valid @RequestBody Place updatedPlace
    ) {
        Place result = placeService.updatePlace(id, updatedPlace);
        return (result != null) ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }

    // 🔒 Upload réservé aux ADMIN ou OWNER du lieu
    @PreAuthorize("hasRole('ADMIN') or (hasRole('OWNER') and @placeService.isOwner(#id, authentication.name))")
    @PostMapping("/{id}/upload-image")
    public ResponseEntity<?> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            String imageUrl = placeService.savePlaceImage(id, file);
            return ResponseEntity.ok(Map.of("url", imageUrl));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erreur lors de l'upload de l'image.");
        } catch (RuntimeException e) { // e.g., Place not found
            return ResponseEntity.notFound().build();
        }
    }
}
