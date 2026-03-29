package com.proximityservice.business.repository;

import com.proximityservice.common.model.GeospatialIndex;
import com.proximityservice.common.model.GeospatialIndexId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeospatialIndexRepository extends JpaRepository<GeospatialIndex, GeospatialIndexId> {
    List<GeospatialIndex> findByBusinessId(Long businessId);
    void deleteByBusinessId(Long businessId);
}
