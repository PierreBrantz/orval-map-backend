package com.orvalmap.service;

import com.orvalmap.model.User;
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.PlaceRequestRepository;
import com.orvalmap.repository.PlaceVisitRepository;
import com.orvalmap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PlaceVisitRepository placeVisitRepository;
    private final PlaceRequestRepository placeRequestRepository;
    private final PlaceRepository placeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public boolean deleteAccount(String username, String currentPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }

        Long userId = user.getId();
        placeVisitRepository.deleteAllByUserId(userId);
        placeRequestRepository.anonymizeByRequesterId(userId);
        placeRepository.removeOwnerByOwnerId(userId);

        // User est propriétaire de la relation ManyToMany : Hibernate supprime
        // également ses lignes dans user_roles avant de supprimer le compte.
        userRepository.delete(user);
        return true;
    }
}
