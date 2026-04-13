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
public class PlaceRequest {

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

    private Double price;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlaceRequestStatus status = PlaceRequestStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "requester_id")
    private User requester; // L'utilisateur qui a fait la suggestion
}
