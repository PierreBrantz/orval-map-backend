package com.orvalmap.service;

import com.orvalmap.model.Place;
import com.orvalmap.model.PlaceRequest;
import com.orvalmap.model.PlaceRequestStatus;
import com.orvalmap.model.User;
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.PlaceRequestRepository;
import com.orvalmap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceRequestService {

    private final PlaceRequestRepository placeRequestRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    public PlaceRequest createRequest(PlaceRequest request, String username) {
        // Trouver l'utilisateur qui fait la demande
        User requester = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        request.setRequester(requester);
        request.setStatus(PlaceRequestStatus.PENDING);
        return placeRequestRepository.save(request);
    }

    public List<PlaceRequest> getAllPendingRequests() {
        return placeRequestRepository.findByStatus(PlaceRequestStatus.PENDING);
    }

    @Transactional
    public Place validateRequest(Long requestId) {
        PlaceRequest request = placeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Requête non trouvée"));

        if (request.getStatus() != PlaceRequestStatus.PENDING) {
            throw new RuntimeException("Cette requête a déjà été traitée");
        }

        // 1. Marquer comme approuvée
        request.setStatus(PlaceRequestStatus.APPROVED);
        placeRequestRepository.save(request);

        // 2. Transformer en vrai Place
        Place newPlace = Place.builder()
                .name(request.getName())
                .city(request.getCity())
                .lat(request.getLat())
                .lng(request.getLng())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .build();

        return placeRepository.save(newPlace);
    }

    public void rejectRequest(Long requestId) {
        PlaceRequest request = placeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Requête non trouvée"));
        request.setStatus(PlaceRequestStatus.REJECTED);
        placeRequestRepository.save(request);
    }
}
