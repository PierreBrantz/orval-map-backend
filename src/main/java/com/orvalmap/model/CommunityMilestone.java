package com.orvalmap.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "community_milestone")
@Getter @Setter
public class CommunityMilestone {
    @Id
    private Long id = 1L;
    // Monotonic: deleting a place must never award the hundredth badge again.
    private long publishedCount;
    // Scalar ID avoids a foreign key preventing account deletion.
    private Long hundredthContributorId;
}
