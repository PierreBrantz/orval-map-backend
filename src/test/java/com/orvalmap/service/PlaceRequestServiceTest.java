package com.orvalmap.service;

import com.cloudinary.Cloudinary;
import com.orvalmap.model.PlaceRequest;
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
}
