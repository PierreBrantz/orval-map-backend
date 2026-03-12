package com.orvalmap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orvalmap.model.Place;
import com.orvalmap.service.PlaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

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
    private PlaceService placeService; // On mock le service, pas le repository

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetAllPlaces() throws Exception {
        Place p1 = new Place("Bar1", "Liège", 50.645, 5.573);
        Place p2 = new Place("Bar2", "Bruxelles", 50.850, 4.350);

        given(placeService.getAllPlaces(any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(Arrays.asList(p1, p2)));

        mockMvc.perform(get("/api/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Bar1"))
                .andExpect(jsonPath("$.content[1].city").value("Bruxelles"));
    }

    @Test
    void testAddPlace() throws Exception {
        Place p = new Place("BarTest", "Namur", 50.467, 4.867);

        given(placeService.addPlace(any(Place.class))).willAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("BarTest"))
                .andExpect(jsonPath("$.city").value("Namur"));
    }

    @Test
    void testGetPlacesByCity_Valid() throws Exception {
        Place p1 = new Place("Bar1", "Liège", 50.645, 5.573);
        Place p2 = new Place("Bar2", "Liège", 50.646, 5.574);

        given(placeService.getAllPlaces(eq("Liège"), any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(Arrays.asList(p1, p2)));

        mockMvc.perform(get("/api/places?city=Liège"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].city").value("Liège"));
    }

    @Test
    void testGetPlacesByCity_NotFound() throws Exception {
        given(placeService.getAllPlaces(eq("Bruxelles"), any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/places?city=Bruxelles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
