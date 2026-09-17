package com.orvalmap.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.orvalmap.exception.DuplicatePlaceException;
import com.orvalmap.model.*;
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.PlaceRequestRepository;
import com.orvalmap.repository.PlaceVisitRepository;
import com.orvalmap.repository.UserRepository;
import com.orvalmap.utils.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlaceRequestService {

    private static final double DUPLICATE_DISTANCE_KM = 0.1;
    private static final String ADMIN_NOTIFICATION_EMAIL = "orvalMaps@gmail.com";

    private final PlaceRequestRepository placeRequestRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final Cloudinary cloudinary;
    private final PlaceVisitRepository placeVisitRepository;
    private final EmailService emailService;

    public PlaceRequest createRequest(PlaceRequestDTO requestDTO, String username) {
        User requester = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        ensureNoPublishedDuplicate(requestDTO.getName(), requestDTO.getLat(), requestDTO.getLng());
        ensureNoPendingDuplicate(requestDTO.getName(), requestDTO.getLat(), requestDTO.getLng());

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

        PlaceRequest savedRequest = placeRequestRepository.save(request);
        notifyAdminOfNewRequest(savedRequest);
        return savedRequest;
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

        // Une autre proposition peut avoir été validée depuis la création de
        // cette demande : on contrôle donc à nouveau les lieux publiés.
        ensureNoPublishedDuplicate(request.getName(), request.getLat(), request.getLng());

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

        notifyRequesterOfApproval(request);

        return newPlace;
    }

    private void notifyRequesterOfApproval(PlaceRequest request) {
        User requester = request.getRequester();
        if (requester == null || requester.getEmail() == null || requester.getEmail().isBlank()) {
            return;
        }

        String emailBody = """
                <p>Bonjour %s,</p>
                <p>Bonne nouvelle ! Votre suggestion <strong>%s</strong> à %s a été validée.</p>
                <p>Elle est maintenant disponible sur OrvalMaps.</p>
                <p>L'équipe OrvalMaps</p>
                """.formatted(
                requester.getUsername(),
                request.getName(),
                request.getCity()
        );

        emailService.sendEmail(
                requester.getEmail(),
                "Votre suggestion a été validée",
                emailBody
        );
    }

    private void notifyAdminOfNewRequest(PlaceRequest request) {
        User requester = request.getRequester();
        String requesterName = requester != null ? requester.getUsername() : "Utilisateur inconnu";
        String requesterEmail = requester != null && requester.getEmail() != null
                ? requester.getEmail()
                : "Non renseignée";
        String price = request.getPrice() != null ? request.getPrice() + " €" : "Non renseigné";

        String emailBody = """
                <p>Une nouvelle suggestion de bar vient d’être envoyée sur OrvalMaps.</p>
                <ul>
                    <li><strong>Nom :</strong> %s</li>
                    <li><strong>Ville :</strong> %s</li>
                    <li><strong>Type :</strong> %s</li>
                    <li><strong>Prix :</strong> %s</li>
                    <li><strong>Coordonnées :</strong> %s, %s</li>
                    <li><strong>Proposée par :</strong> %s (%s)</li>
                </ul>
                <p>Cette suggestion est en attente de validation dans l’espace administrateur.</p>
                """.formatted(
                request.getName(),
                request.getCity(),
                request.getPlaceType(),
                price,
                request.getLat(),
                request.getLng(),
                requesterName,
                requesterEmail
        );

        emailService.sendEmail(
                ADMIN_NOTIFICATION_EMAIL,
                "Nouvelle suggestion de bar à valider : " + request.getName(),
                emailBody
        );
    }

    @Transactional
    public void rejectRequest(Long requestId) {
        PlaceRequest request = placeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Requête non trouvée"));

        if (request.getStatus() != PlaceRequestStatus.PENDING) {
            throw new RuntimeException("Cette requête a déjà été traitée");
        }

        request.setStatus(PlaceRequestStatus.REJECTED);
        placeRequestRepository.save(request);
        notifyRequesterOfRejection(request);
    }

    private void notifyRequesterOfRejection(PlaceRequest request) {
        User requester = request.getRequester();
        if (requester == null || requester.getEmail() == null || requester.getEmail().isBlank()) {
            return;
        }

        String emailBody = """
                <p>Bonjour %s,</p>
                <p>Votre suggestion <strong>%s</strong> à %s a été examinée, mais n’a pas été retenue.</p>
                <p>Elle ne sera donc pas ajoutée à la carte pour le moment.</p>
                <p>Merci pour votre contribution et votre aide à la communauté OrvalMaps.</p>
                <p>L’équipe OrvalMaps</p>
                """.formatted(
                requester.getUsername(),
                request.getName(),
                request.getCity()
        );

        emailService.sendEmail(
                requester.getEmail(),
                "Votre suggestion n’a pas été retenue",
                emailBody
        );
    }

    private void ensureNoPublishedDuplicate(String name, double lat, double lng) {
        String normalizedName = normalizeName(name);
        placeRepository.findAll().stream()
                .filter(place -> normalizeName(place.getName()).equals(normalizedName))
                .filter(place -> GeoUtils.distanceKm(lat, lng, place.getLat(), place.getLng())
                        <= DUPLICATE_DISTANCE_KM)
                .findFirst()
                .ifPresent(place -> {
                    throw new DuplicatePlaceException(
                            "Ce bar existe déjà sur la carte : « " + place.getName() + " ».",
                            "PLACE",
                            place.getId()
                    );
                });
    }

    private void ensureNoPendingDuplicate(String name, double lat, double lng) {
        String normalizedName = normalizeName(name);
        placeRequestRepository.findByStatus(PlaceRequestStatus.PENDING).stream()
                .filter(request -> normalizeName(request.getName()).equals(normalizedName))
                .filter(request -> GeoUtils.distanceKm(lat, lng, request.getLat(), request.getLng())
                        <= DUPLICATE_DISTANCE_KM)
                .findFirst()
                .ifPresent(request -> {
                    throw new DuplicatePlaceException(
                            "Une suggestion pour ce bar est déjà en attente de validation.",
                            "PLACE_REQUEST",
                            request.getId()
                    );
                });
    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }

        String withoutAccents = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutAccents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    public String uploadRequestImage(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "orval-map/requests"
        ));
        return (String) uploadResult.get("secure_url");
    }
}
