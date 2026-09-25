package com.orvalmap.service;
import com.cloudinary.Cloudinary;
import com.orvalmap.model.*;
import com.orvalmap.repository.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class PriceDateTest {
    @Test void editingNamePreservesUnknownPriceDateButChangingPriceSetsDate() {
        PlaceRepository repo = mock(PlaceRepository.class);
        PlaceService service = new PlaceService(repo, mock(Cloudinary.class), mock(PlaceVisitRepository.class), mock(UserRepository.class), mock(VisitService.class));
        Place existing = Place.builder().id(1L).name("Before").price(5.0).build();
        Place update = Place.builder().name("After").price(5.0).build();
        when(repo.findLockedById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(existing)).thenReturn(existing);
        service.updatePlace(1L, update);
        assertThat(existing.getPriceUpdatedAt()).isNull();
        update.setPrice(6.0); service.updatePlace(1L, update);
        assertThat(existing.getPriceUpdatedAt()).isNotNull();
        Instant recorded = existing.getPriceUpdatedAt();
        service.updatePlace(1L, update);
        assertThat(existing.getPriceUpdatedAt()).isEqualTo(recorded);
        update.setPrice(null); service.updatePlace(1L, update);
        assertThat(existing.getPriceUpdatedAt()).isNull();
    }
}
