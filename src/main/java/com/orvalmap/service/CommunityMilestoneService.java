package com.orvalmap.service;
import com.orvalmap.model.CommunityMilestone;
import com.orvalmap.model.User;
import com.orvalmap.repository.CommunityMilestoneRepository;
import com.orvalmap.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityMilestoneService {
    private final CommunityMilestoneRepository milestones;
    private final PlaceRepository places;
    private final JdbcTemplate jdbc;

    // PostgreSQL transaction-scoped lock: serialize additions and prevent a
    // concurrent delete/insert from changing the initial count. Reads stay available.
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockPublication() {
        jdbc.execute("LOCK TABLE place IN SHARE ROW EXCLUSIVE MODE");
    }

    // Called under the publication lock, BEFORE saving the new Place. The counter
    // and attribution roll back together with a failed validation or insertion.
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordPublication(User contributor) {
        CommunityMilestone milestone = milestones.findById(1L).orElseGet(() -> {
            CommunityMilestone initial = new CommunityMilestone();
            initial.setPublishedCount(places.count());
            return initial;
        });
        long next = milestone.getPublishedCount() + 1;
        milestone.setPublishedCount(next);
        if (next == 100 && contributor != null) {
            milestone.setHundredthContributorId(contributor.getId());
        }
        milestones.save(milestone);
    }

    public boolean isHundredthContributor(Long userId) {
        return userId != null && milestones.findById(1L)
                .map(m -> userId.equals(m.getHundredthContributorId())).orElse(false);
    }
}
