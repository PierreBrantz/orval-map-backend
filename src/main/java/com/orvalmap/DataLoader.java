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
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@Profile("dev") // 👈 N'est actif que si le profil 'dev' est sélectionné
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

            // 2. Initialisation des Utilisateurs (Amis & Tests)
            User admin = createUserIfNotFound("admin", "admin123", Set.of(adminRole, ownerRole, userRole));
            User user = createUserIfNotFound("user", "user123", Set.of(userRole));
            User benoit = createUserIfNotFound("benoit", "benoit", Set.of(ownerRole));
            User casto = createUserIfNotFound("casto", "casto", Set.of(ownerRole));
            User david = createUserIfNotFound("david", "david", Set.of(ownerRole));
            User gaelle = createUserIfNotFound("gaelle", "gaelle", Set.of(ownerRole));

            // 3. Initialisation des Lieux (avec prix au hasard)
            createPlaceIfNotFound("L'Ange Gardien", "Orval", 49.638, 5.331,
                    "https://images.unsplash.com/photo-1572116469696-31de0f17cc34?q=80&w=1974&auto=format&fit=crop",
                    benoit, 4.50);

            createPlaceIfNotFound("Le Zig Zag", "Virton", 49.567, 5.533,
                    "https://images.unsplash.com/photo-1514933651103-005eec06c04b?q=80&w=1974&auto=format&fit=crop",
                    benoit, 4.20);

            createPlaceIfNotFound("La Vache à Vin", "Arlon", 49.683, 5.816,
                    "https://images.unsplash.com/photo-1559348349-36dfc68c71ad?q=80&w=1974&auto=format&fit=crop",
                    casto, 5.50);

            createPlaceIfNotFound("Le Pot au Lait", "Liège", 50.642, 5.571,
                    "https://images.unsplash.com/photo-1555953941-b29c3f3a64cb?q=80&w=2070&auto=format&fit=crop",
                    david, 4.80);

            createPlaceIfNotFound("La Maison du Peket", "Liège", 50.644, 5.576,
                    "https://images.unsplash.com/photo-1586996292898-71f4075c6d56?q=80&w=2070&auto=format&fit=crop",
                    david, 5.00);

            createPlaceIfNotFound("BeerLovers Café", "Liège", 50.645, 5.573,
                    "https://fastly.4sqi.net/img/general/600x600/3366366_ZpL-j5qj5qj5qj5qj5qj5qj5qj5qj5qj5qj5qj5qj5.jpg",
                    david, 6.50);

            createPlaceIfNotFound("The Pyg", "Luxembourg-Ville", 49.610, 6.130,
                    "https://images.unsplash.com/photo-1566417713204-38c9e7218e79?q=80&w=2070&auto=format&fit=crop",
                    gaelle, 7.50);

            createPlaceIfNotFound("Urban Bar", "Luxembourg-Ville", 49.611, 6.132,
                    "https://images.unsplash.com/photo-1534078312440-e59c8f8b45d0?q=80&w=2070&auto=format&fit=crop",
                    gaelle, 6.80);

            createPlaceIfNotFound("Scott's Pub", "Luxembourg-Ville", 49.612, 6.135,
                    "https://images.unsplash.com/photo-1597075687490-8f673c6c17f6?q=80&w=1974&auto=format&fit=crop",
                    gaelle, 7.20);

            createPlaceIfNotFound("Delirium Café", "Bruxelles", 50.846, 4.351,
                    "https://images.unsplash.com/photo-1518182170546-07fb612b1850?q=80&w=1974&auto=format&fit=crop",
                    casto, 5.20);

            createPlaceIfNotFound("Moeder Lambic", "Bruxelles", 50.846, 4.346,
                    "https://images.unsplash.com/photo-1572116469696-31de0f17cc34?q=80&w=1974&auto=format&fit=crop",
                    casto, 6.00);

            createPlaceIfNotFound("Le Roy d'Espagne", "Bruxelles", 50.846, 4.352,
                    "https://images.unsplash.com/photo-1555953941-b29c3f3a64cb?q=80&w=2070&auto=format&fit=crop",
                    benoit, 5.80);
        };
    }

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

    private void createPlaceIfNotFound(String name, String city, double lat, double lng, String imageUrl, User owner, double price) {
        if (placeRepository.findAll().stream().noneMatch(p -> p.getName().equals(name) && p.getCity().equals(city))) {
            Place place = Place.builder()
                    .name(name)
                    .city(city)
                    .lat(lat)
                    .lng(lng)
                    .imageUrl(imageUrl)
                    .owner(owner)
                    .price(price)
                    .build();
            placeRepository.save(place);
        }
    }
}
