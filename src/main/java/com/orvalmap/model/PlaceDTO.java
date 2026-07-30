package com.orvalmap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor // Ajout explicite du constructeur sans argument
@AllArgsConstructor // Ajout explicite du constructeur avec tous les arguments
public class PlaceDTO {
    private Long id;
    private String name;
    private String city;
    private double lat;
    private double lng;
    private Double price;
    private String imageUrl;
    private PlaceType placeType;
    private Integer verificationCount;
    private LocalDateTime lastVerificationDate;
    private boolean hasUserVerified;
}
