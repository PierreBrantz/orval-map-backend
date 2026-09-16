package com.orvalmap.service;

import com.orvalmap.model.PassportDTO;
import com.orvalmap.model.PlaceRequestStatus;
import com.orvalmap.model.User;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassportServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PlaceVisitRepository placeVisitRepository;
    @Mock private PlaceRequestRepository placeRequestRepository;

    @InjectMocks
    private PassportService passportService;

    @Test
    void rejectedSuggestionsDoNotIncreasePositiveProgress() {
        User user = User.builder().id(12L).username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(placeVisitRepository.findByUser(user)).thenReturn(List.of());
        when(placeRequestRepository.countByRequesterAndStatus(user, PlaceRequestStatus.APPROVED)).thenReturn(2L);
        when(placeRequestRepository.countByRequesterAndStatus(user, PlaceRequestStatus.PENDING)).thenReturn(1L);
        when(placeRequestRepository.countByRequesterAndStatus(user, PlaceRequestStatus.REJECTED)).thenReturn(4L);

        PassportDTO passport = passportService.getPassportForUser("alice");

        assertThat(passport.getSuggestions().getTotal()).isEqualTo(3);
        assertThat(passport.getSuggestions().getApproved()).isEqualTo(2);
        assertThat(passport.getSuggestions().getPending()).isEqualTo(1);
        assertThat(passport.getSuggestions().getRejected()).isEqualTo(4);
    }
}
