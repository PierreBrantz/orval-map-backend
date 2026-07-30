package com.orvalmap.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceRequestDTO {
    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    @NotBlank(message = "La ville est obligatoire")
    private String city;

    @NotNull(message = "Latitude obligatoire")
    private Double lat;

    @NotNull(message = "Longitude obligatoire")
    private Double lng;

    private Double price;

    private String imageUrl;

    private PlaceType placeType; // Le type est optionnel, le service lui donnera une valeur par défaut si null
}
