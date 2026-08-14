package com.orvalmap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "owner")
    @JsonIgnore
    @Builder.Default
    private Set<Place> ownedPlaces = new HashSet<>();

    // Champs pour le reset de mot de passe
    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    // --- NOUVEAUX CHAMPS POUR LE REFRESH TOKEN ---
    @Column(columnDefinition = "TEXT")
    private String refreshToken;
    private LocalDateTime refreshTokenExpiry;
}
