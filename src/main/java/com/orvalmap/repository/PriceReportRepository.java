package com.orvalmap.repository;

import com.orvalmap.model.PriceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PriceReportRepository extends JpaRepository<PriceReport, Long> {
    List<PriceReport> findByStatusOrderByCreatedAtAsc(String status);
    boolean existsByPlaceIdAndRequesterIdAndStatus(Long placeId, Long requesterId, String status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM PriceReport r WHERE r.id = :id")
    Optional<PriceReport> findLockedById(@Param("id") Long id);
}
