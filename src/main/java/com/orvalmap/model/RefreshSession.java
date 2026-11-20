package com.orvalmap.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Getter @Setter @NoArgsConstructor
public class RefreshSession {
    @Id private String tokenHash;
    @Column(nullable=false) private Long userId;
    @Column(nullable=false) private String passwordFingerprint;
    @Column(nullable=false) private Instant expiresAt;
}
