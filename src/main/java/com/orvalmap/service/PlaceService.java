package com.orvalmap.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.orvalmap.model.*;
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.PlaceVisitRepository;
import com.orvalmap.repository.UserRepository;
import com.orvalmap.utils.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final Cloudinary cloudinary;
    private final PlaceVisitRepository placeVisitRepository;
    private final UserRepository userRepository;

    public Page<PlaceDTO> getAllPlaces(String city, Double lng, Double lat, Double radius, PlaceType placeType, Pageable pageable) {
        
        Page<Place> placesPage;

        if (city != null && !city.isEmpty() && placeType != null) {
            placesPage = placeRepository.findByCityIgnoreCaseAndPlaceType(city, placeType, pageable);
        } else if (city != null && !city.isEmpty()) {
            placesPage = placeRepository.findByCityIgnoreCase(city, pageable);
        } else if (placeType != null) {
            placesPage = placeRepository.findByPlaceType(placeType, pageable);
        } else {
            placesPage = placeRepository.findAll(pageable);
        }

        if (lat != null && lng != null && radius != null) {
            List<Place> geoFilteredPlaces = placesPage.getContent().stream()
                    .filter(p -> GeoUtils.distanceKm(lat, lng, p.getLat(), p.getLng()) <= radius)
                    .collect(Collectors.toList());
            placesPage = new PageImpl<>(geoFilteredPlaces, pageable, placesPage.getTotalElements());
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return placesPage.map(place -> convertToDto(place, Collections.emptySet()));
        }

        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return placesPage.map(place -> convertToDto(place, Collections.emptySet()));
        }

        List<Long> placeIdsOnPage = placesPage.getContent().stream().map(Place::getId).collect(Collectors.toList());
        
        if (placeIdsOnPage.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<Long> visitedPlaceIds = placeVisitRepository.findByUserIdAndPlaceIdIn(currentUser.getId(), placeIdsOnPage)
                .stream()
                .map(visit -> visit.getPlace().getId())
                .collect(Collectors.toSet());

        return placesPage.map(place -> convertToDto(place, visitedPlaceIds));
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
