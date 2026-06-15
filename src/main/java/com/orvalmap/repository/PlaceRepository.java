package com.orvalmap.repository;

import com.orvalmap.model.Place;
import com.orvalmap.model.PlaceType; // Import de PlaceType
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    // Retourne une Page au lieu d'une List
    Page<Place> findByCityIgnoreCase(String city, Pageable pageable);

    // Nouvelle méthode pour le service, qui retourne une List pour le filtrage géographique
    List<Place> findByCityIgnoreCase(String city);

    // Nouvelle méthode pour filtrer par type de lieu
    List<Place> findByPlaceType(PlaceType placeType);

    // Nouvelle méthode pour filtrer par ville et par type de lieu
    List<Place> findByCityIgnoreCaseAndPlaceType(String city, PlaceType placeType);
}
