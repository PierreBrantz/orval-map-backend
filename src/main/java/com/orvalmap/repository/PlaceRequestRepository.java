package com.orvalmap.repository;

import com.orvalmap.model.PlaceRequest;
import com.orvalmap.model.PlaceRequestStatus;
import com.orvalmap.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaceRequestRepository extends JpaRepository<PlaceRequest, Long> {
    List<PlaceRequest> findByStatus(PlaceRequestStatus status);

    long countByRequester(User requester);
    long countByRequesterAndStatus(User requester, PlaceRequestStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PlaceRequest pr SET pr.requester = null WHERE pr.requester.id = :userId")
    void anonymizeByRequesterId(@Param("userId") Long userId);
}
