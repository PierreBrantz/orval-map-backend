package com.orvalmap.service;

import com.orvalmap.model.User;
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.PlaceRequestRepository;
import com.orvalmap.repository.PlaceVisitRepository;
import com.orvalmap.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PlaceVisitRepository placeVisitRepository;
    @Mock
    private PlaceRequestRepository placeRequestRepository;
    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    @Test
    void deletesPersonalRelationsAndAnonymizesCommunityContent() {
        User user = User.builder()
                .id(42L)
                .username("alice")
                .password("encoded-password")
                .build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "encoded-password")).thenReturn(true);

        boolean deleted = accountService.deleteAccount("alice", "current-password");

        assertThat(deleted).isTrue();
        InOrder deletionOrder = inOrder(
                placeVisitRepository, placeRequestRepository, placeRepository, userRepository);
        deletionOrder.verify(placeVisitRepository).deleteAllByUserId(42L);
        deletionOrder.verify(placeRequestRepository).anonymizeByRequesterId(42L);
        deletionOrder.verify(placeRepository).removeOwnerByOwnerId(42L);
        deletionOrder.verify(userRepository).delete(user);
    }

    @Test
    void keepsAccountWhenPasswordIsIncorrect() {
        User user = User.builder()
                .id(42L)
                .username("alice")
                .password("encoded-password")
                .build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        boolean deleted = accountService.deleteAccount("alice", "wrong-password");

        assertThat(deleted).isFalse();
        verify(placeVisitRepository, never()).deleteAllByUserId(42L);
        verify(placeRequestRepository, never()).anonymizeByRequesterId(42L);
        verify(placeRepository, never()).removeOwnerByOwnerId(42L);
        verify(userRepository, never()).delete(user);
    }
}
