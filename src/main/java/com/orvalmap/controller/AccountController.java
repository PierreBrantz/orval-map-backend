package com.orvalmap.controller;

import com.orvalmap.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @DeleteMapping
    public ResponseEntity<?> deleteAccount(
            Authentication authentication,
            @Valid @RequestBody DeleteAccountRequest request
    ) {
        boolean deleted = accountService.deleteAccount(authentication.getName(), request.getPassword());
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Mot de passe incorrect"));
        }

        return ResponseEntity.noContent().build();
    }

    @Data
    public static class DeleteAccountRequest {
        @NotBlank(message = "Le mot de passe est obligatoire")
        private String password;
    }
}
