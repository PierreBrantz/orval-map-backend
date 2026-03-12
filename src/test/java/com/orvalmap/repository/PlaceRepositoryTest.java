package com.orvalmap.repository;

import com.orvalmap.model.Place;
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
        Place place = new Place("Test Bar", "TestCity", 1.234, 5.678);
        repository.save(place);

        List<Place> places = repository.findAll();
        assertThat(places).isNotEmpty();
        assertThat(places).extracting(Place::getName).contains("Test Bar");
    }
}
