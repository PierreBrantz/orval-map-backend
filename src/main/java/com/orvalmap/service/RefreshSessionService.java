package com.orvalmap.service;
import com.orvalmap.model.*;
import com.orvalmap.repository.*;
import com.orvalmap.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class RefreshSessionService {
    private final RefreshSessionRepository sessions;
    private final UserRepository users;
    private final JwtUtil jwt;
    private static final SecureRandom RANDOM = new SecureRandom();
    public record Tokens(String token, String refreshToken) {}
    static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    @Transactional
    public Tokens issue(User user) {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String credential = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshSession session = new RefreshSession();
        session.setTokenHash(hash(credential)); session.setUserId(user.getId());
        session.setPasswordFingerprint(hash(user.getPassword()));
        session.setExpiresAt(Instant.now().plus(Duration.ofDays(90)));
        sessions.save(session);
        return new Tokens(jwt.generateToken(new UserDetailsImpl(user)), credential);
    }
    @Transactional
    public Tokens refresh(String credential) {
        if (credential == null || credential.length() != 43) throw unauthorized();
        RefreshSession session = sessions.findLockedByHash(hash(credential)).orElseThrow(this::unauthorized);
        if (!session.getExpiresAt().isAfter(Instant.now())) throw unauthorized();
        User user = users.findById(session.getUserId()).orElseThrow(this::unauthorized);
        if (!session.getPasswordFingerprint().equals(hash(user.getPassword()))) throw unauthorized();
        session.setExpiresAt(Instant.now().plus(Duration.ofDays(90)));
        sessions.save(session);
        return new Tokens(jwt.generateToken(new UserDetailsImpl(user)), credential);
    }
    @Transactional
    public void revoke(String credential) {
        if (credential != null && credential.length() == 43)
            sessions.findLockedByHash(hash(credential)).ifPresent(sessions::delete);
    }
    private ResponseStatusException unauthorized() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED); }
}
