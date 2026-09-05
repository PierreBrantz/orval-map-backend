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
import org.springframework.data.domain.Sort;
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
    private final VisitService visitService;

    public Page<PlaceDTO> getAllPlaces(String city, Double lng, Double lat, Double radius, PlaceType placeType, Pageable pageable) {
        // La carte doit recevoir tous les lieux correspondants. On conserve une
        // réponse Page pour ne pas casser les clients existants, mais sans tronquer
        // les résultats à la taille de page (20 par défaut).
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("name");
        List<Place> filteredPlaces = placeRepository.findAll(sort).stream()
                .filter(p -> city == null || city.isBlank() || p.getCity().equalsIgnoreCase(city))
                .filter(p -> placeType == null || p.getPlaceType() == placeType)
                .filter(p -> lat == null || lng == null || radius == null
                        || GeoUtils.distanceKm(lat, lng, p.getLat(), p.getLng()) <= radius)
                .collect(Collectors.toList());

        Pageable unpaged = Pageable.unpaged(sort);
        Page<Place> placesPage = new PageImpl<>(filteredPlaces, unpaged, filteredPlaces.size());

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
            return Page.empty(unpaged);
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

    public PlaceDTO getPlaceById(Long id) {
        Place place = placeRepository.findById(id).orElse(null);
        if (place == null) {
            return null;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return convertToDto(place, Collections.emptySet());
        }

        return userRepository.findByUsername(authentication.getName())
                .map(user -> placeVisitRepository.findByUserAndPlace(user, place).isPresent()
                        ? convertToDto(place, Set.of(place.getId()))
                        : convertToDto(place, Collections.emptySet()))
                .orElseGet(() -> convertToDto(place, Collections.emptySet()));
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
        if (!placeRepository.existsById(id)) {
            throw new RuntimeException("Lieu non trouvé avec l'id : " + id);
        }
        
        placeVisitRepository.deleteAllByPlaceId(id);
        placeRepository.deleteById(id);
    }

    public Place updatePlace(Long id, Place updatedPlace) {
        return placeRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedPlace.getName());
                    existing.setCity(updatedPlace.getCity());
                    existing.setLat(updatedPlace.getLat());
                    existing.setLng(updatedPlace.getLng());
                    existing.setPlaceType(updatedPlace.getPlaceType());
                    existing.setPrice(updatedPlace.getPrice());
                    existing.setImageUrl(updatedPlace.getImageUrl());
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

    public boolean isOwner(Long placeId, String username) {
        Place place = placeRepository.findById(placeId).orElse(null);
        if (place == null || place.getOwner() == null) return false;

        return place.getOwner().getUsername().equals(username);
    }
}
