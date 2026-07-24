package com.orvalmap.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
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
    private boolean hasUserVerified; // Nouveau champ pour indiquer si l'utilisateur a vérifié ce lieu
}
