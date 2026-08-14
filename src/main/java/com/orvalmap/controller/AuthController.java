package com.orvalmap.controller;

import com.orvalmap.model.Role;
import com.orvalmap.model.User;
import com.orvalmap.repository.RoleRepository;
import com.orvalmap.repository.UserRepository;
import com.orvalmap.security.JwtUtil;
import com.orvalmap.security.UserDetailsImpl;
import com.orvalmap.service.EmailService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Nom d'utilisateur déjà utilisé");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email déjà utilisé");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_USER")));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(userRole))
                .build();

        user = userRepository.save(user);

        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        String jwtToken = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(jwtToken));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwtToken = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(jwtToken));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .map(user -> {
                    String token = UUID.randomUUID().toString();
                    user.setResetToken(token);
                    user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
                    userRepository.save(user);

                    String resetLink = "https://orvalmaps.com/reset-password?token=" + token;
                    String emailBody = String.format(
                        "<p>Bonjour,</p>" +
                        "<p>Pour réinitialiser votre mot de passe, veuillez cliquer sur le lien ci-dessous :</p>" +
                        "<p><a href=\"%s\">Réinitialiser mon mot de passe</a></p>" +
                        "<p>Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet e-mail.</p>" +
                        "<p>L'équipe OrvalMaps</p>",
                        resetLink
                    );
                    emailService.sendEmail(user.getEmail(), "Réinitialisation de votre mot de passe OrvalMaps", emailBody);

                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        return userRepository.findByResetToken(request.getToken())
                .map(user -> {
                    if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
                        return ResponseEntity.badRequest().body("Le token a expiré.");
                    }

                    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                    user.setResetToken(null);
                    user.setResetTokenExpiry(null);
                    userRepository.save(user);

                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.badRequest().body("Token invalide."));
    }

    // --- DTOs ---
    @Data static class RegisterRequest { @NotBlank private String username; @NotBlank @Email private String email; @NotBlank private String password; }
    @Data static class LoginRequest { private String login; private String password; }
    @Data @AllArgsConstructor static class AuthResponse { private String token; }
    @Data static class ForgotPasswordRequest { @NotBlank @Email private String email; }
    @Data static class ResetPasswordRequest { @NotBlank private String token; @NotBlank private String newPassword; }
}
