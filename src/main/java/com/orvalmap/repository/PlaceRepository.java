package com.orvalmap.repository;

import com.orvalmap.model.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    // Retourne une Page au lieu d'une List
    Page<Place> findByCityIgnoreCase(String city, Pageable pageable);
}
