package com.orvalmap.controller;

import com.orvalmap.model.PassportDTO;
import com.orvalmap.service.PassportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/passport")
@RequiredArgsConstructor
public class PassportController {

    private final PassportService passportService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<PassportDTO> getMyPassport(Authentication authentication) {
        PassportDTO passport = passportService.getPassportForUser(authentication.getName());
        return ResponseEntity.ok(passport);
    }
}
