package com.proximityservice.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "geospatial_index")
@IdClass(GeospatialIndexId.class)
public class GeospatialIndex {
    @Id
    private String geohash;
    @Id
    @Column(name = "business_id")
    private Long businessId;

    public GeospatialIndex() {}

    public GeospatialIndex(String geohash, Long businessId) {
        this.geohash = geohash;
        this.businessId = businessId;
    }

    public String getGeohash() { return geohash; }
    public void setGeohash(String geohash) { this.geohash = geohash; }

    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
}
