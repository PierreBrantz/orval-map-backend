package com.orvalmap.repository;

import com.orvalmap.model.Place;
import com.orvalmap.model.PlaceVisit;
import com.orvalmap.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceVisitRepository extends JpaRepository<PlaceVisit, Long> {
    Optional<PlaceVisit> findByUserAndPlace(User user, Place place);
    List<PlaceVisit> findByUser(User user);
    long countByUser(User user);
}
