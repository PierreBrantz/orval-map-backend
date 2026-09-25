package com.orvalmap.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(indexes = @Index(name = "idx_price_report_status_created", columnList = "status,createdAt"))
public class PriceReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Scalar IDs avoid retaining account details or blocking account/place deletion.
    @Column(nullable = false)
    private Long placeId;
    @Column(nullable = false)
    private Long requesterId;
    private String placeName;
    private String city;
    private Double previousPrice;
    private Instant previousPriceUpdatedAt;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal proposedPrice;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private String status = "PENDING";
}
