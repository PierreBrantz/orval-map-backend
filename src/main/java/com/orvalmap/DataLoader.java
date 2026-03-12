package com.orvalmap;

import com.orvalmap.model.Place;
import com.orvalmap.model.Role;
import com.orvalmap.model.User;
import com.orvalmap.repository.PlaceRepository;
import com.orvalmap.repository.RoleRepository;
import com.orvalmap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataLoader {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {

            // 1. Initialisation des Rôles
            Role adminRole = createRoleIfNotFound("ROLE_ADMIN");
            Role userRole = createRoleIfNotFound("ROLE_USER");
            Role ownerRole = createRoleIfNotFound("ROLE_OWNER");

            // 2. Initialisation des Utilisateurs
            User admin = createUserIfNotFound("admin", "admin123", Set.of(adminRole, ownerRole, userRole));
            User user = createUserIfNotFound("user", "user123", Set.of(userRole));
            User ownerLiege = createUserIfNotFound("owner_liege", "owner123", Set.of(ownerRole));
            User ownerBxl = createUserIfNotFound("owner_bxl", "owner123", Set.of(ownerRole));

            // 3. Initialisation des Lieux (avec images)
            
            // Lieux appartenant à owner_liege
            createPlaceIfNotFound("Le BeerLovers", "Liège", 50.645, 5.573, 
                    "https://images.unsplash.com/photo-1514933651103-005eec06c04b?q=80&w=1974&auto=format&fit=crop", 
                    ownerLiege);

            createPlaceIfNotFound("Bistro Trappiste", "Namur", 50.467, 4.867, 
                    "https://images.unsplash.com/photo-1559348349-36dfc68c71ad?q=80&w=1974&auto=format&fit=crop", 
                    ownerLiege);

            // Lieux appartenant à owner_bxl
            createPlaceIfNotFound("Café Orval", "Bruxelles", 50.850, 4.350, 
                    "https://images.unsplash.com/photo-1555953941-b29c3f3a64cb?q=80&w=2070&auto=format&fit=crop", 
                    ownerBxl);
            
            createPlaceIfNotFound("Moeder Lambic", "Bruxelles", 50.846, 4.346,
                    "https://images.unsplash.com/photo-1572116469696-31de0f17cc34?q=80&w=1974&auto=format&fit=crop",
                    ownerBxl);
        };
    }

    // --- Méthodes utilitaires ---

    private Role createRoleIfNotFound(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(null, name)));
    }

    private User createUserIfNotFound(String username, String rawPassword, Set<Role> roles) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(User.builder()
                        .username(username)
                        .password(passwordEncoder.encode(rawPassword))
                        .roles(roles)
                        .build()));
    }

    private void createPlaceIfNotFound(String name, String city, double lat, double lng, String imageUrl, User owner) {
        // Utilise le nom et la ville pour vérifier l'existence, pour être plus robuste
        if (placeRepository.findAll().stream().noneMatch(p -> p.getName().equals(name) && p.getCity().equals(city))) {
            Place place = Place.builder()
                    .name(name)
                    .city(city)
                    .lat(lat)
                    .lng(lng)
                    .imageUrl(imageUrl)
                    .owner(owner)
                    .build();
            placeRepository.save(place);
        }
    }
}
