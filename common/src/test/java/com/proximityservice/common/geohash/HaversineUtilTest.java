package com.proximityservice.common.geohash;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HaversineUtilTest {

    @Test
    void distance_samePoint_returnsZero() {
        double distance = HaversineUtil.distanceKm(40.7128, -74.0060, 40.7128, -74.0060);
        assertEquals(0.0, distance, 0.001);
    }

    @Test
    void distance_newYorkToStatueOfLiberty_approximately8km() {
        double distance = HaversineUtil.distanceKm(40.7580, -73.9855, 40.6892, -74.0445);
        assertEquals(8.8, distance, 1.0);
    }

    @Test
    void distance_shortDistance_withinOneKm() {
        double distance = HaversineUtil.distanceKm(40.7580, -73.9855, 40.7540, -73.9840);
        assertTrue(distance < 1.0);
        assertTrue(distance > 0.1);
    }

    @Test
    void distance_isSymmetric() {
        double d1 = HaversineUtil.distanceKm(40.7128, -74.0060, 51.5074, -0.1278);
        double d2 = HaversineUtil.distanceKm(51.5074, -0.1278, 40.7128, -74.0060);
        assertEquals(d1, d2, 0.001);
    }

    @Test
    void distance_newYorkToLondon_approximately5570km() {
        double distance = HaversineUtil.distanceKm(40.7128, -74.0060, 51.5074, -0.1278);
        assertEquals(5570.0, distance, 50.0);
    }
}
