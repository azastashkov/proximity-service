package com.proximityservice.common.geohash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GeoHashUtilTest {

    @Test
    void encode_newYorkCity_returnsCorrectGeohash() {
        String hash = GeoHashUtil.encode(40.7580, -73.9855, 6);
        assertEquals("dr5ru7", hash);
    }

    @Test
    void encode_sanFrancisco_returnsCorrectGeohash() {
        String hash = GeoHashUtil.encode(37.7749, -122.4194, 6);
        assertEquals("9q8yyk", hash);
    }

    @Test
    void encode_london_returnsCorrectGeohash() {
        String hash = GeoHashUtil.encode(51.5074, -0.1278, 6);
        assertEquals("gcpvj0", hash);
    }

    @Test
    void encode_precision4_returnsShorterHash() {
        String hash = GeoHashUtil.encode(40.7580, -73.9855, 4);
        assertEquals("dr5r", hash);
    }

    @Test
    void encode_precision5_returnsMediumHash() {
        String hash = GeoHashUtil.encode(40.7580, -73.9855, 5);
        assertEquals("dr5ru", hash);
    }

    @Test
    void getNeighbors_returnsEightNeighbors() {
        List<String> neighbors = GeoHashUtil.getNeighbors("dr5ru7");
        assertEquals(8, neighbors.size());
        assertEquals(8, neighbors.stream().distinct().count());
        assertFalse(neighbors.contains("dr5ru7"));
    }

    @Test
    void getNeighbors_allSameLength() {
        List<String> neighbors = GeoHashUtil.getNeighbors("dr5ru7");
        for (String n : neighbors) {
            assertEquals(6, n.length());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "0.5, 6",
        "1.0, 5",
        "2.0, 5",
        "5.0, 4",
        "20.0, 4"
    })
    void getPrecisionForRadius_returnsCorrectPrecision(double radius, int expected) {
        assertEquals(expected, GeoHashUtil.getPrecisionForRadius(radius));
    }

    @Test
    void encode_equator_primeMeridian() {
        String hash = GeoHashUtil.encode(0.0, 0.0, 6);
        assertNotNull(hash);
        assertEquals(6, hash.length());
    }

    @Test
    void getSearchGeohashes_returnsNineCells() {
        List<String> cells = GeoHashUtil.getSearchGeohashes(40.7580, -73.9855, 5.0);
        assertEquals(9, cells.size());
    }
}
