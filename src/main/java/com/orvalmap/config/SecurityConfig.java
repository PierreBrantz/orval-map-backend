package com.orvalmap.config;

import com.orvalmap.security.CustomAuthenticationEntryPoint;
import com.orvalmap.security.JwtAuthFilter;
import com.orvalmap.security.JwtUtil;
import com.orvalmap.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final Environment environment;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint; // Injection ajoutée

    public SecurityConfig(
                          UserDetailsServiceImpl userDetailsService,
                          Environment environment,
                          CustomAuthenticationEntryPoint customAuthenticationEntryPoint) { // Paramètre ajouté
        this.userDetailsService = userDetailsService;
        this.environment = environment;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint; // Initialisation
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService) {
        return new JwtAuthFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, @Lazy JwtAuthFilter jwtAuthFilter) throws Exception {

        boolean isDev = environment.matchesProfiles("dev");

        http.csrf(csrf -> csrf.disable());
        http.headers(headers -> {
            headers.frameOptions(frame -> frame.disable());
        });
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // 🚪 Règles d'accès
        http.authorizeHttpRequests(auth -> {
            // Endpoints publics
            auth.requestMatchers("/api/auth/**").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/api/places/**").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/uploads/**").permitAll();
            auth.requestMatchers("/error").permitAll(); // Toujours autoriser l'affichage des erreurs
            auth.requestMatchers("/privacy.html", "/terms.html", "/static/**").permitAll(); // Permettre l'accès aux fichiers statiques

            if (isDev) {
                auth.requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/h2-console/**"
                ).permitAll();
            }

            // Tout le reste sécurisé
            auth.anyRequest().authenticated();
        });

        // Configuration du CustomAuthenticationEntryPoint
        http.exceptionHandling(exception -> exception.authenticationEntryPoint(customAuthenticationEntryPoint)); // Ligne ajoutée

        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    private UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
