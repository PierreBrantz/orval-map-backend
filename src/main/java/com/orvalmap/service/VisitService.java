package com.orvalmap.service;

import com.orvalmap.model.Place;
import com.orvalmap.model.PlaceVisit;
import com.orvalmap.model.User;
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.PlaceVisitRepository;
import com.orvalmap.repository.UserRepository;
import com.orvalmap.utils.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitService {

    private final PlaceVisitRepository visitRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;

    private static final double MAX_DISTANCE_KM = 0.2; // 200 mètres

    @Transactional
    public PlaceVisit markAsVisited(Long placeId, String username, double userLat, double userLng) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("Lieu non trouvé"));

        log.info("--- DEBUG: Vérification de la visite ---");
        log.info("Utilisateur: {} ({})", user.getUsername(), user.getId());
        log.info("Lieu: {} ({})", place.getName(), place.getId());
        log.info("Coordonnées utilisateur (reçues): lat={}, lng={}", userLat, userLng);
        log.info("Coordonnées du lieu (DB): lat={}, lng={}", place.getLat(), place.getLng());

        double distance = GeoUtils.distanceKm(userLat, userLng, place.getLat(), place.getLng());
        log.info("Distance calculée: {} km", distance);

        if (distance > MAX_DISTANCE_KM) {
            log.warn("Visite refusée. Distance ({}) > Seuil ({})", distance, MAX_DISTANCE_KM);
            throw new IllegalStateException("Vous n'êtes pas assez proche du lieu pour le marquer comme visité.");
        }

        log.info("Visite acceptée. Distance ({}) <= Seuil ({})", distance, MAX_DISTANCE_KM);

        PlaceVisit visit = visitRepository.findByUserAndPlace(user, place)
                .orElseGet(() -> PlaceVisit.builder()
                        .user(user)
                        .place(place)
                        .build());

        visit.setVisitedAt(LocalDateTime.now());
        return visitRepository.save(visit);
    }

    @Transactional
    public void removeFromVisited(Long placeId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("Lieu non trouvé"));

        visitRepository.findByUserAndPlace(user, place)
                .ifPresent(visitRepository::delete);
    }
}
