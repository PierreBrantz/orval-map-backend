package com.orvalmap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orvalmap.model.Place;
import com.orvalmap.model.PlaceCreationDTO;
import com.orvalmap.model.PlaceDTO;
import com.orvalmap.model.PlaceType;
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
    private PlaceService placeService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetAllPlaces() throws Exception {
        PlaceDTO p1Dto = PlaceDTO.builder()
                .id(1L).name("Bar1").city("Liège").lat(50.645).lng(5.573).placeType(PlaceType.BAR).hasUserVerified(false).build();
        PlaceDTO p2Dto = PlaceDTO.builder()
                .id(2L).name("Bar2").city("Bruxelles").lat(50.850).lng(4.350).placeType(PlaceType.BAR).hasUserVerified(false).build();

        given(placeService.getAllPlaces(any(), any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(Arrays.asList(p1Dto, p2Dto)));

        mockMvc.perform(get("/api/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Bar1"))
                .andExpect(jsonPath("$.content[1].city").value("Bruxelles"));
    }

    @Test
    void testAddPlace() throws Exception {
        PlaceCreationDTO creationDTO = new PlaceCreationDTO();
        creationDTO.setName("BarTest");
        creationDTO.setCity("Namur");
        creationDTO.setLat(50.467);
        creationDTO.setLng(4.867);
        creationDTO.setPlaceType(PlaceType.BAR);

        Place savedPlace = Place.builder()
                .id(1L)
                .name(creationDTO.getName())
                .city(creationDTO.getCity())
                .lat(creationDTO.getLat())
                .lng(creationDTO.getLng())
                .placeType(creationDTO.getPlaceType())
                .build();

        given(placeService.addPlace(any(PlaceCreationDTO.class))).willReturn(savedPlace);

        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creationDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("BarTest"))
                .andExpect(jsonPath("$.city").value("Namur"))
                .andExpect(jsonPath("$.placeType").value("BAR"));
    }

    @Test
    void testGetPlacesByCity_Valid() throws Exception {
        PlaceDTO p1Dto = PlaceDTO.builder()
                .id(1L).name("Bar1").city("Liège").lat(50.645).lng(5.573).placeType(PlaceType.BAR).hasUserVerified(false).build();
        PlaceDTO p2Dto = PlaceDTO.builder()
                .id(2L).name("Bar2").city("Liège").lat(50.646).lng(5.574).placeType(PlaceType.BAR).hasUserVerified(false).build();

        given(placeService.getAllPlaces(eq("Liège"), any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(Arrays.asList(p1Dto, p2Dto)));

        mockMvc.perform(get("/api/places?city=Liège"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].city").value("Liège"));
    }

    @Test
    void testGetPlacesByCity_NotFound() throws Exception {
        given(placeService.getAllPlaces(eq("Bruxelles"), any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/places?city=Bruxelles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
