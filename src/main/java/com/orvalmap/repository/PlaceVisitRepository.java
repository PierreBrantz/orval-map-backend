package com.orvalmap.repository;

import com.orvalmap.model.Place;
import com.orvalmap.model.PlaceVisit;
import com.orvalmap.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface PlaceVisitRepository extends JpaRepository<PlaceVisit, Long> {
    Optional<PlaceVisit> findByUserAndPlace(User user, Place place);
    List<PlaceVisit> findByUser(User user);
    long countByUser(User user);

    // --- NOUVELLE APPROCHE PLUS SIMPLE ---
    List<PlaceVisit> findByUserIdAndPlaceIdIn(Long userId, List<Long> placeIds);

    // On peut garder l'ancienne méthode pour référence si besoin, mais la nouvelle est plus sûre
    @Query("SELECT pv.place.id FROM PlaceVisit pv WHERE pv.user.id = :userId AND pv.place.id IN :placeIds")
    Set<Long> findVisitedPlaceIdsByUserAndPlaceIds(@Param("userId") Long userId, @Param("placeIds") List<Long> placeIds);
}
