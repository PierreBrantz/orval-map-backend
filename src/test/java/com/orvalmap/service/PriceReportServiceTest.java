package com.orvalmap.service;

import com.orvalmap.model.*;
import com.orvalmap.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceReportServiceTest {
    @Mock PlaceRepository places;
    @Mock PriceReportRepository reports;
    @Mock UserRepository users;
    @InjectMocks PriceReportService service;

    Place place() { return Place.builder().id(1L).name("Cafe").city("Florenville").price(5.0).build(); }
    PriceReport report() {
        PriceReport r = new PriceReport(); r.setId(7L); r.setPlaceId(1L); r.setPreviousPrice(5.0);
        r.setProposedPrice(new BigDecimal("6.50")); r.setCreatedAt(Instant.parse("2026-09-01T10:00:00Z"));
        return r;
    }
    void submission(Place p) {
        User u = new User(); u.setId(2L);
        when(users.findByUsername("alice")).thenReturn(Optional.of(u));
        when(places.findLockedById(1L)).thenReturn(Optional.of(p));
    }
    @Test void submissionDoesNotChangePublishedPrice() {
        Place p = place(); submission(p);
        service.submit(1L, new BigDecimal("6.50"), "alice");
        assertThat(p.getPrice()).isEqualTo(5.0); assertThat(p.getPriceUpdatedAt()).isNull();
        verify(places, never()).save(any());
        ArgumentCaptor<PriceReport> c = ArgumentCaptor.forClass(PriceReport.class);
        verify(reports).save(c.capture());
        assertThat(c.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(c.getValue().getPreviousPrice()).isEqualTo(5.0);
        assertThat(c.getValue().getCreatedAt()).isNotNull();
    }
    @Test void duplicatePendingReportIsRejected() {
        submission(place()); when(reports.existsByPlaceIdAndRequesterIdAndStatus(1L, 2L, "PENDING")).thenReturn(true);
        assertThatThrownBy(() -> service.submit(1L, new BigDecimal("6.50"), "alice"))
            .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        verify(reports, never()).save(any());
    }
    @Test void rejectsInvalidPrices() {
        for (String amount : new String[]{"0", "-1", "1000", "4.567"}) {
            assertThatThrownBy(() -> service.submit(1L, new BigDecimal(amount), "alice"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        }
        verifyNoInteractions(places, reports);
    }
    @Test void unchangedPriceIsRejected() {
        submission(place());
        assertThatThrownBy(() -> service.submit(1L, new BigDecimal("5.00"), "alice"))
            .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
    }
    @Test void approvalUsesObservationDateRatherThanApprovalDate() {
        Place p = place(); PriceReport r = report();
        when(reports.findLockedById(7L)).thenReturn(Optional.of(r));
        when(places.findLockedById(1L)).thenReturn(Optional.of(p));
        service.decide(7L, true);
        assertThat(p.getPrice()).isEqualTo(6.5);
        assertThat(p.getPriceUpdatedAt()).isEqualTo(r.getCreatedAt());
        assertThat(r.getStatus()).isEqualTo("APPROVED");
    }
    @Test void rejectionLeavesPriceAndDateUnchanged() {
        PriceReport r = report(); when(reports.findLockedById(7L)).thenReturn(Optional.of(r));
        service.decide(7L, false);
        verifyNoInteractions(places); assertThat(r.getStatus()).isEqualTo("REJECTED");
    }
    @Test void stalePriceCannotOverwriteNewerPrice() {
        Place p = place(); p.setPrice(7.0); PriceReport r = report();
        when(reports.findLockedById(7L)).thenReturn(Optional.of(r));
        when(places.findLockedById(1L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.decide(7L, true)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        assertThat(p.getPrice()).isEqualTo(7.0); assertThat(r.getStatus()).isEqualTo("PENDING");
        verify(places, never()).save(any());
    }
    @Test void changedDateAlsoMakesReportStale() {
        Place p = place(); p.setPriceUpdatedAt(Instant.now()); PriceReport r = report();
        when(reports.findLockedById(7L)).thenReturn(Optional.of(r));
        when(places.findLockedById(1L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.decide(7L, true)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        verify(places, never()).save(any());
    }
    @Test void alreadyProcessedReportCannotBeAppliedAgain() {
        PriceReport r = report(); r.setStatus("APPROVED"); when(reports.findLockedById(7L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.decide(7L, true)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        verifyNoInteractions(places);
    }
    @Test void deletedPlaceCannotBeUpdated() {
        when(reports.findLockedById(7L)).thenReturn(Optional.of(report()));
        assertThatThrownBy(() -> service.decide(7L, true)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verify(places, never()).save(any());
    }
}
