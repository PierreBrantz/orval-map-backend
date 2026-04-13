package com.orvalmap.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    @NotBlank(message = "La ville est obligatoire")
    private String city;

    @NotNull(message = "Latitude obligatoire")
    private double lat;

    @NotNull(message = "Longitude obligatoire")
    private double lng;

    private Double price; // Prix de l'Orval

    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    public Place(String name, String city, double lat, double lng) {
        this.name = name;
        this.city = city;
        this.lat = lat;
        this.lng = lng;
    }
}
