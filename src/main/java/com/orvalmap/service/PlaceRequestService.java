package com.orvalmap.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.orvalmap.model.*; // Import de tous les modèles
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.PlaceRequestRepository;
import com.orvalmap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Ajout de l'import pour le logging
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j // Ajout de l'annotation pour le logging
public class PlaceRequestService {

    private final PlaceRequestRepository placeRequestRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final Cloudinary cloudinary;

    public PlaceRequest createRequest(PlaceRequestDTO requestDTO, String username) {
        User requester = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        PlaceRequest request = PlaceRequest.builder()
                .name(requestDTO.getName())
                .city(requestDTO.getCity())
                .lat(requestDTO.getLat())
                .lng(requestDTO.getLng())
                .price(requestDTO.getPrice())
                .imageUrl(requestDTO.getImageUrl())
                .placeType(requestDTO.getPlaceType() != null ? requestDTO.getPlaceType() : PlaceType.BAR) // Valeur par défaut ici
                .requester(requester)
                .status(PlaceRequestStatus.PENDING)
                .build();

        // --- LIGNE DE DÉBOGAGE ---
        log.info("Avant sauvegarde, PlaceType de la requête : {}", request.getPlaceType());

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

        request.setStatus(PlaceRequestStatus.APPROVED);
        placeRequestRepository.save(request);

        Place newPlace = Place.builder()
                .name(request.getName())
                .city(request.getCity())
                .lat(request.getLat())
                .lng(request.getLng())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .placeType(request.getPlaceType())
                .build();

        return placeRepository.save(newPlace);
    }

    public void rejectRequest(Long requestId) {
        PlaceRequest request = placeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Requête non trouvée"));
        request.setStatus(PlaceRequestStatus.REJECTED);
        placeRequestRepository.save(request);
    }

    // ✅ Nouvel upload pour les suggestions (sans ID de lieu encore existant)
    public String uploadRequestImage(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "orval-map/requests"
        ));
        return (String) uploadResult.get("secure_url");
    }
}
