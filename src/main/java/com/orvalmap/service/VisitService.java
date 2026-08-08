package com.orvalmap.service;

import com.orvalmap.model.Place;
import com.orvalmap.model.PlaceVisit;
import com.orvalmap.model.User;
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.PlaceVisitRepository;
import com.orvalmap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final PlaceVisitRepository visitRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;

    @Transactional
    public PlaceVisit markAsVisited(Long placeId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("Lieu non trouvé"));

        // La contrainte unique dans la DB empêche les doublons
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
