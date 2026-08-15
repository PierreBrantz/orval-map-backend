package com.orvalmap.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.orvalmap.model.*;
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.PlaceRequestRepository;
import com.orvalmap.repository.PlaceVisitRepository;
import com.orvalmap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlaceRequestService {

    private final PlaceRequestRepository placeRequestRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final Cloudinary cloudinary;
    private final PlaceVisitRepository placeVisitRepository;

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
                .placeType(requestDTO.getPlaceType() != null ? requestDTO.getPlaceType() : PlaceType.BAR)
                .requester(requester)
                .status(PlaceRequestStatus.PENDING)
                .build();

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
        
        placeRepository.save(newPlace);

        PlaceVisit visit = PlaceVisit.builder()
                .user(request.getRequester())
                .place(newPlace)
                .visitedAt(LocalDateTime.now())
                .build();
        placeVisitRepository.save(visit);

        return newPlace;
    }

    public void rejectRequest(Long requestId) {
        PlaceRequest request = placeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Requête non trouvée"));
        request.setStatus(PlaceRequestStatus.REJECTED);
        placeRequestRepository.save(request);
    }

    public String uploadRequestImage(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "orval-map/requests"
        ));
        return (String) uploadResult.get("secure_url");
    }
}
