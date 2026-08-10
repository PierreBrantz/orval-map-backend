package com.orvalmap.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.orvalmap.model.*; // Import de tous les modèles
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.PlaceVisitRepository;
import com.orvalmap.repository.UserRepository;
import com.orvalmap.repository.UserPlaceVerificationRepository;
import com.orvalmap.utils.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final Cloudinary cloudinary;
    private final PlaceVisitRepository placeVisitRepository;
    private final UserRepository userRepository;
    private final UserPlaceVerificationRepository userPlaceVerificationRepository;
    private final VisitService visitService; // Ajout du VisitService

    public Page<PlaceDTO> getAllPlaces(String city, Double lng, Double lat, Double radius, PlaceType placeType, Pageable pageable) {
        
        Page<Place> placesPage;

        boolean isGeoSearch = lat != null && lng != null && radius != null;

        if (isGeoSearch) {
            List<Place> allPlaces;
            if (city != null && !city.isEmpty() && placeType != null) {
                allPlaces = placeRepository.findByCityIgnoreCaseAndPlaceType(city, placeType);
            } else if (city != null && !city.isEmpty()) {
                allPlaces = placeRepository.findByCityIgnoreCase(city);
            } else if (placeType != null) {
                allPlaces = placeRepository.findByPlaceType(placeType);
            } else {
                allPlaces = placeRepository.findAll();
            }

            List<Place> filteredPlaces = allPlaces.stream()
                    .filter(p -> GeoUtils.distanceKm(lat, lng, p.getLat(), p.getLng()) <= radius)
                    .collect(Collectors.toList());
            
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredPlaces.size());
            
            if (start > filteredPlaces.size()) {
                placesPage = new PageImpl<>(Collections.emptyList(), pageable, filteredPlaces.size());
            } else {
                placesPage = new PageImpl<>(filteredPlaces.subList(start, end), pageable, filteredPlaces.size());
            }

        } else {
            if (city != null && !city.isEmpty() && placeType != null) {
                placesPage = placeRepository.findByCityIgnoreCaseAndPlaceType(city, placeType, pageable);
            } else if (city != null && !city.isEmpty()) {
                placesPage = placeRepository.findByCityIgnoreCase(city, pageable);
            } else if (placeType != null) {
                placesPage = placeRepository.findByPlaceType(placeType, pageable);
            } else {
                placesPage = placeRepository.findAll(pageable);
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Set<Long> userVisitedPlaceIds;

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (currentUser != null) {
                List<Long> placeIds = placesPage.getContent().stream().map(Place::getId).collect(Collectors.toList());
                userVisitedPlaceIds = placeVisitRepository.findVisitedPlaceIdsByUserAndPlaceIds(currentUser.getId(), placeIds);
            } else {
                userVisitedPlaceIds = Collections.emptySet();
            }
        } else {
            userVisitedPlaceIds = Collections.emptySet();
        }

        return placesPage.map(place -> convertToDto(place, userVisitedPlaceIds));
    }

    private PlaceDTO convertToDto(Place place, Set<Long> userVisitedPlaceIds) {
        PlaceDTO dto = new PlaceDTO();
        dto.setId(place.getId());
        dto.setName(place.getName());
        dto.setCity(place.getCity());
        dto.setLat(place.getLat());
        dto.setLng(place.getLng());
        dto.setPrice(place.getPrice());
        dto.setImageUrl(place.getImageUrl());
        dto.setPlaceType(place.getPlaceType());
        dto.setVerificationCount(place.getVerificationCount());
        dto.setLastVerificationDate(place.getLastVerificationDate());
        dto.setHasUserVerified(userVisitedPlaceIds.contains(place.getId()));
        return dto;
    }

    public Place getPlaceById(Long id) {
        return placeRepository.findById(id).orElse(null);
    }

    public Place addPlace(PlaceCreationDTO placeCreationDTO) {
        Place place = Place.builder()
                .name(placeCreationDTO.getName())
                .city(placeCreationDTO.getCity())
                .lat(placeCreationDTO.getLat())
                .lng(placeCreationDTO.getLng())
                .price(placeCreationDTO.getPrice())
                .placeType(placeCreationDTO.getPlaceType() != null ? placeCreationDTO.getPlaceType() : PlaceType.BAR)
                .build();
        return placeRepository.save(place);
    }

    @Transactional
    public void deletePlace(Long id) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lieu non trouvé avec l'id : " + id));
        
        userPlaceVerificationRepository.deleteAllByPlace(place);
        
        place.setOwner(null);
        placeRepository.delete(place);
    }

    public Place updatePlace(Long id, Place updatedPlace) {
        return placeRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedPlace.getName());
                    existing.setCity(updatedPlace.getCity());
                    existing.setLat(updatedPlace.getLat());
                    existing.setLng(updatedPlace.getLng());
                    existing.setPlaceType(updatedPlace.getPlaceType());
                    return placeRepository.save(existing);
                })
                .orElse(null);
    }

    @Transactional
    public PlaceVisit verifyPlace(Long placeId, String username) {
        // --- CORRECTION : Déléguer au VisitService ---
        return visitService.markAsVisited(placeId, username);
    }

    public String savePlaceImage(Long placeId, MultipartFile file) throws IOException {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("Place not found with id: " + placeId));

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "orval-map/places"
        ));

        String imageUrl = (String) uploadResult.get("secure_url");

        place.setImageUrl(imageUrl);
        placeRepository.save(place);

        return imageUrl;
    }
}
