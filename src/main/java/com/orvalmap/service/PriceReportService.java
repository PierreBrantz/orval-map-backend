package com.orvalmap.service;

import com.orvalmap.model.*;
import com.orvalmap.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service @RequiredArgsConstructor
public class PriceReportService {
    private final PlaceRepository places;
    private final PriceReportRepository reports;
    private final UserRepository users;

    @Transactional
    public void submit(Long placeId, BigDecimal price, String username) {
        if (price == null || price.signum() <= 0 || price.compareTo(new BigDecimal("999.99")) > 0 || price.scale() > 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        User user = users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        // Serializes concurrent submissions for the same place, including duplicate checks.
        Place place = places.findLockedById(placeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (reports.existsByPlaceIdAndRequesterIdAndStatus(placeId, user.getId(), "PENDING"))
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        if (place.getPrice() != null && Double.compare(place.getPrice(), price.doubleValue()) == 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        PriceReport report = new PriceReport();
        report.setPlaceId(placeId);
        report.setRequesterId(user.getId());
        report.setPlaceName(place.getName());
        report.setCity(place.getCity());
        report.setPreviousPrice(place.getPrice());
        report.setPreviousPriceUpdatedAt(place.getPriceUpdatedAt());
        report.setProposedPrice(price);
        report.setCreatedAt(Instant.now());
        reports.save(report);
    }

    @Transactional(readOnly = true)
    public List<PriceReport> pending() { return reports.findByStatusOrderByCreatedAtAsc("PENDING"); }

    @Transactional
    public void decide(Long id, boolean approve) {
        PriceReport report = reports.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"PENDING".equals(report.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT);
        if (approve) {
            Place place = places.findLockedById(report.getPlaceId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!Objects.equals(place.getPrice(), report.getPreviousPrice()) ||
                    !Objects.equals(place.getPriceUpdatedAt(), report.getPreviousPriceUpdatedAt()))
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            place.setPrice(report.getProposedPrice().doubleValue());
            // Keep the reporting date: approval must not make an old observation look recent.
            place.setPriceUpdatedAt(report.getCreatedAt());
            places.save(place);
        }
        report.setStatus(approve ? "APPROVED" : "REJECTED");
        reports.save(report);
    }
}
