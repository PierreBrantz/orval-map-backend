package com.orvalmap.repository;

import com.orvalmap.model.Place;
import com.orvalmap.model.User;
import com.orvalmap.model.UserPlaceVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserPlaceVerificationRepository extends JpaRepository<UserPlaceVerification, Long> {

    // Trouve la dernière vérification d'un utilisateur pour un lieu donné après une certaine date
    Optional<UserPlaceVerification> findTopByVerifierAndPlaceAndVerificationDateAfterOrderByVerificationDateDesc(
            User verifier, Place place, LocalDateTime date
    );

    // Nouvelle méthode pour trouver les IDs des lieux vérifiés par un utilisateur
    @Query("SELECT upv.place.id FROM UserPlaceVerification upv WHERE upv.verifier.id = :userId AND upv.place.id IN :placeIds")
    Set<Long> findVerifiedPlaceIdsByUserAndPlaceIds(@Param("userId") Long userId, @Param("placeIds") List<Long> placeIds);
}
