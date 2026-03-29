package com.proximityservice.common.model;

import java.io.Serializable;
import java.util.Objects;

public class GeospatialIndexId implements Serializable {
    private String geohash;
    private Long businessId;

    public GeospatialIndexId() {}

    public GeospatialIndexId(String geohash, Long businessId) {
        this.geohash = geohash;
        this.businessId = businessId;
    }

    public String getGeohash() { return geohash; }
    public void setGeohash(String geohash) { this.geohash = geohash; }

    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeospatialIndexId)) return false;
        GeospatialIndexId that = (GeospatialIndexId) o;
        return Objects.equals(geohash, that.geohash) && Objects.equals(businessId, that.businessId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(geohash, businessId);
    }
}
