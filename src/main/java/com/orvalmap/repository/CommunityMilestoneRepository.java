package com.orvalmap.repository;
import com.orvalmap.model.CommunityMilestone;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CommunityMilestoneRepository extends JpaRepository<CommunityMilestone, Long> {
    @Modifying
    @Query("UPDATE CommunityMilestone m SET m.hundredthContributorId = null WHERE m.hundredthContributorId = :userId")
    void anonymizeContributor(@Param("userId") Long userId);
}
