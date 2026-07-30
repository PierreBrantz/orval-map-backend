package com.orvalmap.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.orvalmap.model.*; // Import de tous les modèles
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.UserPlaceVerificationRepository;
import com.orvalmap.repository.UserRepository;
import com.orvalmap.utils.GeoUtils;
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
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final Cloudinary cloudinary;
    private final UserPlaceVerificationRepository userPlaceVerificationRepository;
    private final UserRepository userRepository;

    public PlaceService(PlaceRepository placeRepository, Cloudinary cloudinary,
                        UserPlaceVerificationRepository userPlaceVerificationRepository,
                        UserRepository userRepository) {
        this.placeRepository = placeRepository;
        this.cloudinary = cloudinary;
        this.userPlaceVerificationRepository = userPlaceVerificationRepository;
        this.userRepository = userRepository;
    }

    public Page<PlaceDTO> getAllPlaces(String city, Double lng, Double lat, Double radius, PlaceType placeType, Pageable pageable) {
        
        Page<Place> placesPage = placeRepository.findAll(pageable); // Récupère une page de lieux

        // Récupère l'utilisateur actuel s'il est authentifié
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        }

        // Récupère les vérifications de l'utilisateur actuel pour les lieux de la page
        Set<Long> userVerifiedPlaceIds = Collections.emptySet();
        if (currentUser != null) {
            List<Long> placeIds = placesPage.getContent().stream().map(Place::getId).collect(Collectors.toList());
            userVerifiedPlaceIds = userPlaceVerificationRepository.findVerifiedPlaceIdsByUserAndPlaceIds(currentUser.getId(), placeIds);
        }

        // Convertit la page de Place en page de PlaceDTO
        Page<PlaceDTO> placesDtoPage = placesPage.map(place -> convertToDto(place, userVerifiedPlaceIds));

        return placesDtoPage;
    }

    private PlaceDTO convertToDto(Place place, Set<Long> userVerifiedPlaceIds) {
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
        dto.setHasUserVerified(userVerifiedPlaceIds.contains(place.getId())); // Définit si l'utilisateur a vérifié ce lieu
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
                .placeType(placeCreationDTO.getPlaceType() != null ? placeCreationDTO.getPlaceType() : PlaceType.BAR) // Valeur par défaut ici
                .build();
        return placeRepository.save(place);
    }

    @Transactional
    public void deletePlace(Long id) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lieu non trouvé avec l'id : " + id));
        
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

    public Place verifyPlace(Long placeId, String username) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new RuntimeException("Lieu non trouvé avec l'id : " + placeId));

        User verifier = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + username));

        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        boolean alreadyVerifiedRecently = userPlaceVerificationRepository
                .findTopByVerifierAndPlaceAndVerificationDateAfterOrderByVerificationDateDesc(verifier, place, twentyFourHoursAgo)
                .isPresent();

        if (alreadyVerifiedRecently) {
            throw new RuntimeException("Vous avez déjà vérifié ce lieu au cours des dernières 24 heures.");
        }

        UserPlaceVerification newVerification = UserPlaceVerification.builder()
                .verifier(verifier)
                .place(place)
                .verificationDate(LocalDateTime.now())
                .build();
        userPlaceVerificationRepository.save(newVerification);

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
