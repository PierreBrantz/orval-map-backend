package com.orvalmap.repository;

import com.orvalmap.model.Place;
import com.orvalmap.model.PlaceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class PlaceRepositoryTest {

    @Autowired
    private PlaceRepository repository;

    @Test
    void testSaveAndFindPlace() {
        // Utilisation du builder pour créer une instance de Place
        Place place = Place.builder()
                .name("Test Bar")
                .city("TestCity")
                .lat(1.234)
                .lng(5.678)
                .placeType(PlaceType.BAR) // Spécifier un PlaceType
                .build();
        repository.save(place);

        List<Place> places = repository.findAll();
        assertThat(places).isNotEmpty();
        assertThat(places).extracting(Place::getName).contains("Test Bar");
        assertThat(places).extracting(Place::getPlaceType).contains(PlaceType.BAR);
    }
}
