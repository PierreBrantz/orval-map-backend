package com.orvalmap.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlaceRequestService {

    private final PlaceRequestRepository placeRequestRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final Cloudinary cloudinary;

    public PlaceRequest createRequest(PlaceRequest request, String username) {
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

        request.setStatus(PlaceRequestStatus.APPROVED);
        placeRequestRepository.save(request);

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

    // ✅ Nouvel upload pour les suggestions (sans ID de lieu encore existant)
    public String uploadRequestImage(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "orval-map/requests"
        ));
        return (String) uploadResult.get("secure_url");
    }
}
