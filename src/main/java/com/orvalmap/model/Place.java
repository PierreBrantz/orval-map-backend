package com.orvalmap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    private Double price;

    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    // --- Changé int en Integer pour éviter l'erreur NOT NULL en prod ---
    @Builder.Default
    private Integer verificationCount = 0;

    private LocalDateTime lastVerificationDate;

    @Enumerated(EnumType.STRING) // Stocke le nom de l'énumération en tant que chaîne de caractères
    @Builder.Default // Valeur par défaut pour les nouvelles instances créées avec @Builder
    private PlaceType placeType = PlaceType.BAR; // Valeur par défaut BAR

    // Nouvelle relation pour gérer la suppression en cascade des vérifications
    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    @Builder.Default
    private Set<UserPlaceVerification> verifications = new HashSet<>();

    public Place(String name, String city, double lat, double lng) {
        this.name = name;
        this.city = city;
        this.lat = lat;
        this.lng = lng;
        this.placeType = PlaceType.BAR; // Assure une valeur par défaut pour ce constructeur aussi
    }
}
