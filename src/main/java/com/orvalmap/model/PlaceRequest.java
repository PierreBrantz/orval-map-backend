package com.orvalmap.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    private String submittedBy; // Optionnel : nom de l'utilisateur qui a fait la proposition

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
