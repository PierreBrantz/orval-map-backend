package com.orvalmap.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.orvalmap.model.Place;
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.utils.GeoUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final Cloudinary cloudinary;

    public PlaceService(PlaceRepository placeRepository, Cloudinary cloudinary) {
        this.placeRepository = placeRepository;
        this.cloudinary = cloudinary;
    }

    public Page<Place> getAllPlaces(String city, Double lng, Double lat, Double radius, Pageable pageable) {
        // 1. Cas simple : Filtrage par ville (paginé par la DB)
        if (city != null && !city.isEmpty()) {
            return placeRepository.findByCityIgnoreCase(city, pageable);
        }

        // 2. Cas complexe : Filtrage par rayon (pagination en mémoire)
        if (lat != null && lng != null && radius != null) {
            // On récupère TOUT (attention à la perf si > 10k lieux, mais ok pour commencer)
            List<Place> allPlaces = placeRepository.findAll();
            
            double finalLat = lat;
            double finalLng = lng;
            double finalRadius = radius;

            List<Place> filteredPlaces = allPlaces.stream()
                    .filter(p -> GeoUtils.distanceKm(finalLat, finalLng, p.getLat(), p.getLng()) <= finalRadius)
                    .collect(Collectors.toList());

            // Pagination manuelle de la liste filtrée
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredPlaces.size());

            if (start > filteredPlaces.size()) {
                return new PageImpl<>(Collections.emptyList(), pageable, filteredPlaces.size());
            }
            
            List<Place> pageContent = filteredPlaces.subList(start, end);
            return new PageImpl<>(pageContent, pageable, filteredPlaces.size());
        }

        // 3. Cas par défaut : Tout récupérer (paginé par la DB)
        return placeRepository.findAll(pageable);
    }

    public Place getPlaceById(Long id) {
        return placeRepository.findById(id).orElse(null);
    }

    public Place addPlace(Place place) {
        return placeRepository.save(place);
    }

    public void deletePlace(Long id) {
        placeRepository.deleteById(id);
    }

    public Place updatePlace(Long id, Place updatedPlace) {
        return placeRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedPlace.getName());
                    existing.setCity(updatedPlace.getCity());
                    existing.setLat(updatedPlace.getLat());
                    existing.setLng(updatedPlace.getLng());
                    return placeRepository.save(existing);
                })
                .orElse(null);
    }

    public boolean isOwner(Long placeId, String username) {
        Place place = placeRepository.findById(placeId).orElse(null);
        if (place == null || place.getOwner() == null) return false;

        return place.getOwner().getUsername().equals(username);
    }

    public String savePlaceImage(Long placeId, MultipartFile file) throws IOException {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("Place not found with id: " + placeId));

        // Upload vers Cloudinary
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "orval-map/places" // Dossier dans Cloudinary
        ));

        // Récupérer l'URL sécurisée (https)
        String imageUrl = (String) uploadResult.get("secure_url");

        place.setImageUrl(imageUrl);
        placeRepository.save(place);

        return imageUrl;
    }
}
