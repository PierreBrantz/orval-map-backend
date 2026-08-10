package com.orvalmap.controller;

import com.orvalmap.model.Place;
import com.orvalmap.model.PlaceCreationDTO;
import com.orvalmap.model.PlaceDTO;
import com.orvalmap.model.PlaceType;
import com.orvalmap.model.PlaceVisit;
import com.orvalmap.service.PlaceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @GetMapping
    public Page<PlaceDTO> getAllPlaces(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) PlaceType placeType,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return placeService.getAllPlaces(city, lng, lat, radius, placeType, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Place> getPlaceById(@PathVariable Long id) {
        Place place = placeService.getPlaceById(id);
        return (place != null) ? ResponseEntity.ok(place) : ResponseEntity.notFound().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/verify")
    public ResponseEntity<?> verifyPlace(@PathVariable Long id, Authentication authentication) {
        try {
            PlaceVisit visit = placeService.verifyPlace(id, authentication.getName());
            return ResponseEntity.ok(visit.getPlace()); // Retourne l'entité Place pour la compatibilité
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Lieu non trouvé")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Vous avez déjà vérifié ce lieu")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
            }
            return ResponseEntity.internalServerError().body("Une erreur inattendue est survenue: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Place addPlace(@Valid @RequestBody PlaceCreationDTO placeCreationDTO) {
        return placeService.addPlace(placeCreationDTO);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlace(@PathVariable Long id) {
        placeService.deletePlace(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('OWNER') and @placeService.isOwner(#id, authentication.name))")
    @PutMapping("/{id}")
    public ResponseEntity<Place> updatePlace(
            @PathVariable Long id,
            @Valid @RequestBody Place updatedPlace
    ) {
        Place result = placeService.updatePlace(id, updatedPlace);
        return (result != null) ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }

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
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
