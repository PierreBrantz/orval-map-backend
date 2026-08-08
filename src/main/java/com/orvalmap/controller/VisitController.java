package com.orvalmap.controller;

import com.orvalmap.model.PlaceVisit;
import com.orvalmap.service.VisitService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/places/{placeId}/visit") // Groupé par 'places' pour la cohérence
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<VisitResponse> markAsVisited(@PathVariable Long placeId, Authentication authentication) {
        PlaceVisit visit = visitService.markAsVisited(placeId, authentication.getName());
        return ResponseEntity.ok(new VisitResponse(visit.getPlace().getId(), true, visit.getVisitedAt()));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping
    public ResponseEntity<Void> removeFromVisited(@PathVariable Long placeId, Authentication authentication) {
        visitService.removeFromVisited(placeId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Data
    private static class VisitResponse {
        private final Long placeId;
        private final boolean visited;
        private final LocalDateTime visitedAt;
    }
}
