package com.orvalmap.repository;

import com.orvalmap.model.PlaceRequest;
import com.orvalmap.model.PlaceRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceRequestRepository extends JpaRepository<PlaceRequest, Long> {
    List<PlaceRequest> findByStatus(PlaceRequestStatus status);
}
