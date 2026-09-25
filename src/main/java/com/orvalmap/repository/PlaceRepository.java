package com.orvalmap.repository;

import com.orvalmap.model.Place;
import com.orvalmap.model.PlaceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Place p WHERE p.id = :id")
    java.util.Optional<Place> findLockedById(@Param("id") Long id);

    Page<Place> findByCityIgnoreCase(String city, Pageable pageable);

    // Changé pour retourner une Page
    Page<Place> findByPlaceType(PlaceType placeType, Pageable pageable);

    // Changé pour retourner une Page
    Page<Place> findByCityIgnoreCaseAndPlaceType(String city, PlaceType placeType, Pageable pageable);

    // Gardé pour les cas où une List est nécessaire (filtrage géographique manuel)
    List<Place> findByCityIgnoreCase(String city);
    List<Place> findByPlaceType(PlaceType placeType);
    List<Place> findByCityIgnoreCaseAndPlaceType(String city, PlaceType placeType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Place p SET p.owner = null WHERE p.owner.id = :ownerId")
    void removeOwnerByOwnerId(@Param("ownerId") Long ownerId);
}
