package com.orvalmap.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.orvalmap.model.Place;
import com.orvalmap.model.User;
import com.orvalmap.model.UserPlaceVerification;
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.UserPlaceVerificationRepository;
import com.orvalmap.repository.UserRepository;
import com.orvalmap.utils.GeoUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final Cloudinary cloudinary;
    private final UserPlaceVerificationRepository userPlaceVerificationRepository; // Nouvelle injection
    private final UserRepository userRepository; // Nouvelle injection

    public PlaceService(PlaceRepository placeRepository, Cloudinary cloudinary,
                        UserPlaceVerificationRepository userPlaceVerificationRepository, // Nouveau paramètre
                        UserRepository userRepository) { // Nouveau paramètre
        this.placeRepository = placeRepository;
        this.cloudinary = cloudinary;
        this.userPlaceVerificationRepository = userPlaceVerificationRepository; // Initialisation
        this.userRepository = userRepository; // Initialisation
    }

    public Page<Place> getAllPlaces(String city, Double lng, Double lat, Double radius, Pageable pageable) {
        if (city != null && !city.isEmpty()) {
            return placeRepository.findByCityIgnoreCase(city, pageable);
        }

        if (lat != null && lng != null && radius != null) {
            List<Place> allPlaces = placeRepository.findAll();
            
            double finalLat = lat;
            double finalLng = lng;
            double finalRadius = radius;

            List<Place> filteredPlaces = allPlaces.stream()
                    .filter(p -> GeoUtils.distanceKm(finalLat, finalLng, p.getLat(), p.getLng()) <= finalRadius)
                    .collect(Collectors.toList());

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredPlaces.size());

            if (start > filteredPlaces.size()) {
                return new PageImpl<>(Collections.emptyList(), pageable, filteredPlaces.size());
            }
            
            List<Place> pageContent = filteredPlaces.subList(start, end);
            return new PageImpl<>(pageContent, pageable, filteredPlaces.size());
        }

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

    // ✅ Méthode pour confirmer la présence d'un Orval, limitée à une fois par 24h par utilisateur
    public Place verifyPlace(Long placeId, String username) { // Ajout de placeId et username
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("Lieu non trouvé avec l'id : " + placeId));

        User verifier = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + username));

        // Vérifier si l'utilisateur a déjà vérifié ce lieu au cours des dernières 24 heures
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        boolean alreadyVerifiedRecently = userPlaceVerificationRepository
                .findTopByVerifierAndPlaceAndVerificationDateAfterOrderByVerificationDateDesc(verifier, place, twentyFourHoursAgo)
                .isPresent();

        if (alreadyVerifiedRecently) {
            throw new RuntimeException("Vous avez déjà vérifié ce lieu au cours des dernières 24 heures.");
        }

        // Enregistrer la nouvelle vérification
        UserPlaceVerification newVerification = UserPlaceVerification.builder()
                .verifier(verifier)
                .place(place)
                .verificationDate(LocalDateTime.now())
                .build();
        userPlaceVerificationRepository.save(newVerification);

        // Mettre à jour le Place
        place.setVerificationCount(place.getVerificationCount() + 1);
        place.setLastVerificationDate(LocalDateTime.now());
        return placeRepository.save(place);
    }

    public boolean isOwner(Long placeId, String username) {
        Place place = placeRepository.findById(placeId).orElse(null);
        if (place == null || place.getOwner() == null) return false;

        return place.getOwner().getUsername().equals(username);
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
