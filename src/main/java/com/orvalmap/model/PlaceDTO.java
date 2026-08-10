package com.orvalmap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDTO {
    private Long id;
    private String name;
    private String city;
    private double lat;
    private double lng;
    private Double price;
    private String imageUrl;
    private PlaceType placeType;
    private boolean hasUserVerified;
}
