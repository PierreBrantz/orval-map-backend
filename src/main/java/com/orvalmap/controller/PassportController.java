package com.orvalmap.controller;

import com.orvalmap.model.PassportDTO;
import com.orvalmap.model.PlaceVisit;
import com.orvalmap.service.PassportService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class PassportController {

    private final PassportService passportService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/passport")
    public ResponseEntity<PassportDTO> getMyPassport(Authentication authentication) {
        PassportDTO passport = passportService.getPassportForUser(authentication.getName());
        return ResponseEntity.ok(passport);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/visits")
    public ResponseEntity<VisitsListResponse> getMyVisits(Authentication authentication) {
        List<PlaceVisit> visits = passportService.getVisitsForUser(authentication.getName());
        return ResponseEntity.ok(new VisitsListResponse(visits));
    }

    // --- DTOs de Réponse ---

    @Data
    public static class VisitsListResponse {
        private final long count;
        private final List<VisitedPlaceDTO> places;

        public VisitsListResponse(List<PlaceVisit> visits) {
            this.count = visits.size();
            this.places = visits.stream()
                    .map(VisitedPlaceDTO::new)
                    .collect(Collectors.toList());
        }
    }

    @Data
    @Builder
    public static class VisitedPlaceDTO {
        private Long id;
        private String name;
        private String city;
        private double lat;
        private double lng;
        private LocalDateTime visitedAt;

        public VisitedPlaceDTO(PlaceVisit visit) {
            this.id = visit.getPlace().getId();
            this.name = visit.getPlace().getName();
            this.city = visit.getPlace().getCity();
            this.lat = visit.getPlace().getLat();
            this.lng = visit.getPlace().getLng();
            this.visitedAt = visit.getVisitedAt();
        }
    }
}
