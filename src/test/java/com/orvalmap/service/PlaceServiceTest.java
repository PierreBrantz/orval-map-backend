package com.orvalmap.service;

import com.cloudinary.Cloudinary;
import com.orvalmap.model.Place;
import com.orvalmap.model.PlaceDTO;
import com.orvalmap.model.PlaceType;
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.PlaceVisitRepository;
import com.orvalmap.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private Cloudinary cloudinary;
    @Mock
    private PlaceVisitRepository placeVisitRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VisitService visitService;

    @InjectMocks
    private PlaceService placeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAllPlacesEvenWhenRequestedPageSizeIsTwenty() {
        List<Place> places = new ArrayList<>();
        for (long id = 1; id <= 25; id++) {
            places.add(place(id, "Bar " + id, 50.0, 5.0));
        }
        when(placeRepository.findAll(any(Sort.class))).thenReturn(places);

        Page<PlaceDTO> result = placeService.getAllPlaces(
                null, null, null, null, PlaceType.BAR, PageRequest.of(0, 20, Sort.by("name")));

        assertThat(result.getContent()).hasSize(25);
        assertThat(result.getTotalElements()).isEqualTo(25);
    }

    @Test
    void appliesRadiusToAllPlacesInsteadOfOnlyTheFirstPage() {
        List<Place> places = new ArrayList<>();
        for (long id = 1; id <= 20; id++) {
            places.add(place(id, "Bar lointain " + id, 48.8566, 2.3522));
        }
        places.add(place(21, "Le Porthuis", 50.8503, 4.3517));
        when(placeRepository.findAll(any(Sort.class))).thenReturn(places);

        Page<PlaceDTO> result = placeService.getAllPlaces(
                null, 4.3517, 50.8503, 2.0, PlaceType.BAR, PageRequest.of(0, 20, Sort.by("name")));

        assertThat(result.getContent())
                .extracting(PlaceDTO::getName)
                .containsExactly("Le Porthuis");
    }

    private Place place(long id, String name, double lat, double lng) {
        return Place.builder()
                .id(id)
                .name(name)
                .city("Bruxelles")
                .lat(lat)
                .lng(lng)
                .placeType(PlaceType.BAR)
                .build();
    }
}
