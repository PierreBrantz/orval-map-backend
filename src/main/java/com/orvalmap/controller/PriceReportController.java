package com.orvalmap.controller;

import com.orvalmap.service.PriceReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/price-reports")
public class PriceReportController {
    private final PriceReportService service;
    public record Submission(@NotNull @Positive Long placeId,
            @NotNull @DecimalMin("0.01") @DecimalMax("999.99") @Digits(integer = 3, fraction = 2) BigDecimal proposedPrice) {}
    public record Pending(Long id, Long placeId, String placeName, String city, Double previousPrice,
                          BigDecimal proposedPrice, Instant createdAt) {}

    @PostMapping @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> submit(@Valid @RequestBody Submission request, Authentication authentication) {
        service.submit(request.placeId(), request.proposedPrice(), authentication.getName());
        return ResponseEntity.status(201).build();
    }
    @GetMapping("/pending") @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public List<Pending> pending() {
        return service.pending().stream().map(r -> new Pending(r.getId(), r.getPlaceId(), r.getPlaceName(),
                r.getCity(), r.getPreviousPrice(), r.getProposedPrice(), r.getCreatedAt())).toList();
    }
    @PostMapping("/{id}/validate") @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Void> approve(@PathVariable Long id) { service.decide(id, true); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/reject") @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Void> reject(@PathVariable Long id) { service.decide(id, false); return ResponseEntity.noContent().build(); }
}
