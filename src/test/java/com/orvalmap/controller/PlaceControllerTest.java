package com.orvalmap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orvalmap.model.Place;
import com.orvalmap.repository.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlaceController.class)
public class PlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlaceRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetAllPlaces() throws Exception {
        Place p1 = new Place("Bar1", "Liège", 50.645, 5.573);
        Place p2 = new Place("Bar2", "Bruxelles", 50.850, 4.350);

        given(repository.findAll()).willReturn(Arrays.asList(p1, p2));

        mockMvc.perform(get("/api/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bar1"))
                .andExpect(jsonPath("$[1].city").value("Bruxelles"));
    }

    @Test
    void testAddPlace() throws Exception {
        Place p = new Place("BarTest", "Namur", 50.467, 4.867);

        given(repository.save(any(Place.class))).willAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("BarTest"))
                .andExpect(jsonPath("$.city").value("Namur"));
    }

    @Test
    void testAddPlaceInvalid() throws Exception {
        // Place sans nom
        Place p = new Place("", "Namur", 50.467, 4.867);

        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetPlaceById_Valid() throws Exception {
        Place p = new Place("BarID", "Liège", 50.645, 5.573);
        p.setId(1L);

        given(repository.findById(eq(1L))).willReturn(Optional.of(p));

        mockMvc.perform(get("/api/places/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("BarID"))
                .andExpect(jsonPath("$.city").value("Liège"));
    }

    @Test
    void testGetPlaceById_NotFound() throws Exception {
        given(repository.findById(anyLong())).willReturn(Optional.empty());

        mockMvc.perform(get("/api/places/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetPlacesByCity_Valid() throws Exception {
        Place p1 = new Place("Bar1", "Liège", 50.645, 5.573);
        Place p2 = new Place("Bar2", "Liège", 50.646, 5.574);

        // Mock du repository pour retourner uniquement les lieux de Liège
        given(repository.findByCityIgnoreCase("Liège")).willReturn(Arrays.asList(p1, p2));

        mockMvc.perform(get("/api/places?city=Liège"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].city").value("Liège"))
                .andExpect(jsonPath("$[1].city").value("Liège"));
    }

    @Test
    void testGetPlacesByCity_NotFound() throws Exception {
        given(repository.findByCityIgnoreCase("Bruxelles")).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/places?city=Bruxelles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetPlacesByDistance_Valid() throws Exception {
        Place p1 = new Place("Bar1", "Liège", 50.645, 5.573);
        Place p2 = new Place("Bar2", "Liège", 50.646, 5.574);

        double centerLat = 50.645;
        double centerLng = 5.573;
        double radiusKm = 2.0;

        // Mock du repository pour renvoyer uniquement les lieux proches
        given(repository.findAll()).willReturn(Arrays.asList(p1, p2));

        mockMvc.perform(get("/api/places?lat={lat}&lng={lng}&radius={radius}", centerLat, centerLng, radiusKm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testGetPlacesByDistance_NoneFound() throws Exception {
        double centerLat = 50.000;
        double centerLng = 5.000;
        double radiusKm = 1.0;

        given(repository.findAll()).willReturn(Arrays.asList(
                new Place("Bar1", "Liège", 50.645, 5.573),
                new Place("Bar2", "Liège", 50.646, 5.574)
        ));

        mockMvc.perform(get("/api/places?lat={lat}&lng={lng}&radius={radius}", centerLat, centerLng, radiusKm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

}
