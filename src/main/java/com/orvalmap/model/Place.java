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

    // Nouvelle relation pour gérer la suppression en cascade des vérifications
    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // Évite la boucle infinie lors de la sérialisation JSON
    @ToString.Exclude // Évite la boucle infinie dans les logs/toString()
    @Builder.Default // Initialise la collection pour éviter les NullPointerExceptions
    private Set<UserPlaceVerification> verifications = new HashSet<>();

    public Place(String name, String city, double lat, double lng) {
        this.name = name;
        this.city = city;
        this.lat = lat;
        this.lng = lng;
    }
}
