package com.orvalmap.repository;

import com.orvalmap.model.Place;
import com.orvalmap.model.User;
import com.orvalmap.model.UserPlaceVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserPlaceVerificationRepository extends JpaRepository<UserPlaceVerification, Long> {

    // Trouve la dernière vérification d'un utilisateur pour un lieu donné après une certaine date
    Optional<UserPlaceVerification> findTopByVerifierAndPlaceAndVerificationDateAfterOrderByVerificationDateDesc(
            User verifier, Place place, LocalDateTime date
    );
}
