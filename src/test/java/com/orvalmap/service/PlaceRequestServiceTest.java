package com.orvalmap.service;

import com.cloudinary.Cloudinary;
import com.orvalmap.exception.DuplicatePlaceException;
import com.orvalmap.model.Place;
import com.orvalmap.model.PlaceRequest;
import com.orvalmap.model.PlaceRequestDTO;
import com.orvalmap.model.PlaceRequestStatus;
import com.orvalmap.model.User;
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.PlaceRequestRepository;
import com.orvalmap.repository.PlaceVisitRepository;
import com.orvalmap.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceRequestServiceTest {

    @Mock private PlaceRequestRepository placeRequestRepository;
    @Mock private PlaceRepository placeRepository;
    @Mock private UserRepository userRepository;
    @Mock private Cloudinary cloudinary;
    @Mock private PlaceVisitRepository placeVisitRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private PlaceRequestService placeRequestService;

    @Test
    void rejectsRequestMatchingPublishedPlaceAfterNameNormalization() {
        User requester = User.builder().username("alice").build();
        Place existingPlace = Place.builder()
                .id(10L)
                .name("Le-Pórthuis")
                .lat(50.8500)
                .lng(4.3500)
                .build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(requester));
        when(placeRepository.findAll()).thenReturn(List.of(existingPlace));

        PlaceRequestDTO request = request("  LE   PORTHUIS ", 50.8503, 4.3500);

        assertThatThrownBy(() -> placeRequestService.createRequest(request, "alice"))
                .isInstanceOfSatisfying(DuplicatePlaceException.class, exception -> {
                    assertThat(exception.getMessage()).isEqualTo(
                            "Ce bar existe déjà sur la carte : « Le-Pórthuis ».");
                    assertThat(exception.getDuplicateType()).isEqualTo("PLACE");
                    assertThat(exception.getDuplicateId()).isEqualTo(10L);
                });

        verify(placeRequestRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsRequestMatchingPendingRequestWithinOneHundredMeters() {
        User requester = User.builder().username("alice").build();
        PlaceRequest pendingRequest = PlaceRequest.builder()
                .id(20L)
                .name("Le Porthuis")
                .lat(50.8500)
                .lng(4.3500)
                .status(PlaceRequestStatus.PENDING)
                .build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(requester));
        when(placeRequestRepository.findByStatus(PlaceRequestStatus.PENDING))
                .thenReturn(List.of(pendingRequest));

        PlaceRequestDTO request = request("le porthuis", 50.8503, 4.3500);

        assertThatThrownBy(() -> placeRequestService.createRequest(request, "alice"))
                .isInstanceOfSatisfying(DuplicatePlaceException.class, exception -> {
                    assertThat(exception.getMessage()).isEqualTo(
                            "Une suggestion pour ce bar est déjà en attente de validation.");
                    assertThat(exception.getDuplicateType()).isEqualTo("PLACE_REQUEST");
                    assertThat(exception.getDuplicateId()).isEqualTo(20L);
                });
    }

    @Test
    void allowsSameNormalizedNameWhenCoordinatesAreFarApart() {
        User requester = User.builder().username("alice").build();
        Place existingPlace = Place.builder()
                .id(10L)
                .name("Le Porthuis")
                .lat(50.8500)
                .lng(4.3500)
                .build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(requester));
        when(placeRepository.findAll()).thenReturn(List.of(existingPlace));
        when(placeRequestRepository.findByStatus(PlaceRequestStatus.PENDING)).thenReturn(List.of());
        when(placeRequestRepository.save(org.mockito.ArgumentMatchers.any(PlaceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlaceRequest saved = placeRequestService.createRequest(
                request("Le Porthuis", 50.9000, 4.3500), "alice");

        assertThat(saved.getStatus()).isEqualTo(PlaceRequestStatus.PENDING);
        verify(placeRequestRepository).save(saved);
    }

    @Test
    void checksPublishedPlacesAgainBeforeValidation() {
        PlaceRequest pendingRequest = PlaceRequest.builder()
                .id(7L)
                .name("Le Porthuis")
                .lat(50.8500)
                .lng(4.3500)
                .status(PlaceRequestStatus.PENDING)
                .build();
        Place existingPlace = Place.builder()
                .id(10L)
                .name("le-porthuis")
                .lat(50.8501)
                .lng(4.3500)
                .build();
        when(placeRequestRepository.findById(7L)).thenReturn(Optional.of(pendingRequest));
        when(placeRepository.findAll()).thenReturn(List.of(existingPlace));

        assertThatThrownBy(() -> placeRequestService.validateRequest(7L))
                .isInstanceOf(DuplicatePlaceException.class)
                .hasMessage("Ce bar existe déjà sur la carte : « le-porthuis ».");

        assertThat(pendingRequest.getStatus()).isEqualTo(PlaceRequestStatus.PENDING);
        verify(placeRequestRepository, never()).save(pendingRequest);
    }

    @Test
    void rejectsPendingRequestAndSendsNegativeEmail() {
        User requester = User.builder()
                .username("alice")
                .email("alice@example.com")
                .build();
        PlaceRequest request = PlaceRequest.builder()
                .id(7L)
                .name("Bar test")
                .city("Namur")
                .requester(requester)
                .status(PlaceRequestStatus.PENDING)
                .build();
        when(placeRequestRepository.findById(7L)).thenReturn(Optional.of(request));

        placeRequestService.rejectRequest(7L);

        assertThat(request.getStatus()).isEqualTo(PlaceRequestStatus.REJECTED);
        verify(placeRequestRepository).save(request);
        verify(emailService).sendEmail(
                eq("alice@example.com"),
                eq("Votre suggestion n’a pas été retenue"),
                contains("Bar test")
        );
    }

    @Test
    void cannotRejectAnAlreadyProcessedRequest() {
        PlaceRequest request = PlaceRequest.builder()
                .id(7L)
                .status(PlaceRequestStatus.APPROVED)
                .build();
        when(placeRequestRepository.findById(7L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> placeRequestService.rejectRequest(7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cette requête a déjà été traitée");

        verify(placeRequestRepository, never()).save(request);
        verify(emailService, never()).sendEmail(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private PlaceRequestDTO request(String name, double lat, double lng) {
        PlaceRequestDTO request = new PlaceRequestDTO();
        request.setName(name);
        request.setCity("Bruxelles");
        request.setLat(lat);
        request.setLng(lng);
        return request;
    }
}
