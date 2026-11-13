package com.orvalmap.service;
import com.orvalmap.model.*;
import com.orvalmap.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CommunityMilestoneServiceTest {
    @Mock CommunityMilestoneRepository milestones;
    @Mock PlaceRepository places;
    @Mock JdbcTemplate jdbc;
    @InjectMocks CommunityMilestoneService service;

    @Test void hundredthPublicationAwardsTheContributor() {
        when(places.count()).thenReturn(99L);
        service.lockPublication();
        service.recordPublication(User.builder().id(42L).build());
        ArgumentCaptor<CommunityMilestone> saved = ArgumentCaptor.forClass(CommunityMilestone.class);
        verify(milestones).save(saved.capture());
        assertThat(saved.getValue().getPublishedCount()).isEqualTo(100);
        assertThat(saved.getValue().getHundredthContributorId()).isEqualTo(42L);
        verify(jdbc).execute("LOCK TABLE place IN SHARE ROW EXCLUSIVE MODE");
    }
    @Test void alreadyPassedMilestoneIsNotAssignedToANewUser() {
        when(places.count()).thenReturn(100L);
        service.recordPublication(User.builder().id(42L).build());
        ArgumentCaptor<CommunityMilestone> saved = ArgumentCaptor.forClass(CommunityMilestone.class);
        verify(milestones).save(saved.capture());
        assertThat(saved.getValue().getHundredthContributorId()).isNull();
    }
    @Test void laterPublicationPreservesWinnerEvenIfPlacesWereDeleted() {
        CommunityMilestone milestone = new CommunityMilestone();
        milestone.setPublishedCount(100); milestone.setHundredthContributorId(42L);
        when(milestones.findById(1L)).thenReturn(Optional.of(milestone));
        service.recordPublication(User.builder().id(43L).build());
        assertThat(milestone.getPublishedCount()).isEqualTo(101);
        assertThat(milestone.getHundredthContributorId()).isEqualTo(42L);
        verifyNoInteractions(places);
        assertThat(service.isHundredthContributor(42L)).isTrue();
        assertThat(service.isHundredthContributor(43L)).isFalse();
    }
    @Test void ninetyNinthDoesNotAwardBadge() {
        when(places.count()).thenReturn(98L);
        service.recordPublication(User.builder().id(42L).build());
        ArgumentCaptor<CommunityMilestone> saved = ArgumentCaptor.forClass(CommunityMilestone.class);
        verify(milestones).save(saved.capture());
        assertThat(saved.getValue().getPublishedCount()).isEqualTo(99);
        assertThat(saved.getValue().getHundredthContributorId()).isNull();
    }
}
