package com.proximityservice.lbs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximityservice.common.dto.NearbySearchResponse;
import com.proximityservice.common.model.Business;
import com.proximityservice.common.model.GeospatialIndex;
import com.proximityservice.lbs.repository.BusinessRepository;
import com.proximityservice.lbs.repository.GeospatialIndexRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NearbySearchServiceTest {

    @Mock
    private GeospatialIndexRepository geoRepository;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NearbySearchService nearbySearchService;

    // SF coordinates
    private static final double LAT = 37.7749;
    private static final double LON = -122.4194;
    private static final double RADIUS = 5.0;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        doNothing().when(valueOperations).set(anyString(), anyString(), any());
    }

    private Business buildBusiness(Long id, String name, double lat, double lon) {
        Business b = new Business();
        b.setId(id);
        b.setName(name);
        b.setLatitude(lat);
        b.setLongitude(lon);
        b.setCategory("restaurant");
        return b;
    }

    @Test
    void searchNearby_withResults_returnsFilteredBusinesses() {
        // Business close to search point (within 5km)
        Business business = buildBusiness(1L, "Close Business", 37.7800, -122.4100);

        // Mock geo index entries
        List<GeospatialIndex> geoEntries = List.of(new GeospatialIndex("9q8y", 1L));
        when(geoRepository.findByGeohashIn(anyList())).thenReturn(geoEntries);
        when(businessRepository.findAllById(anyList())).thenReturn(List.of(business));

        NearbySearchResponse response = nearbySearchService.searchNearby(LAT, LON, RADIUS);

        assertThat(response).isNotNull();
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.businesses()).hasSize(1);
        assertThat(response.businesses().get(0).name()).isEqualTo("Close Business");
    }

    @Test
    void searchNearby_noResults_returnsEmptyList() {
        when(geoRepository.findByGeohashIn(anyList())).thenReturn(List.of());

        NearbySearchResponse response = nearbySearchService.searchNearby(LAT, LON, RADIUS);

        assertThat(response).isNotNull();
        assertThat(response.total()).isEqualTo(0);
        assertThat(response.businesses()).isEmpty();
        verifyNoInteractions(businessRepository);
    }

    @Test
    void searchNearby_businessOutsideRadius_isFiltered() {
        // Business far away (e.g., New York)
        Business farBusiness = buildBusiness(2L, "Far Business", 40.7128, -74.0060);

        List<GeospatialIndex> geoEntries = List.of(new GeospatialIndex("9q8y", 2L));
        when(geoRepository.findByGeohashIn(anyList())).thenReturn(geoEntries);
        when(businessRepository.findAllById(anyList())).thenReturn(List.of(farBusiness));

        NearbySearchResponse response = nearbySearchService.searchNearby(LAT, LON, RADIUS);

        assertThat(response).isNotNull();
        assertThat(response.total()).isEqualTo(0);
        assertThat(response.businesses()).isEmpty();
    }

    @Test
    void searchNearby_resultsAreSortedByDistance() {
        // closer: ~0.8km away
        Business closer = buildBusiness(1L, "Closer Business", 37.7820, -122.4194);
        // farther: ~3km away
        Business farther = buildBusiness(2L, "Farther Business", 37.7479, -122.4194);

        List<GeospatialIndex> geoEntries = List.of(
            new GeospatialIndex("9q8y", 1L),
            new GeospatialIndex("9q8y", 2L)
        );
        when(geoRepository.findByGeohashIn(anyList())).thenReturn(geoEntries);
        when(businessRepository.findAllById(anyList())).thenReturn(List.of(closer, farther));

        NearbySearchResponse response = nearbySearchService.searchNearby(LAT, LON, RADIUS);

        assertThat(response.businesses()).hasSize(2);
        assertThat(response.businesses().get(0).name()).isEqualTo("Closer Business");
        assertThat(response.businesses().get(1).name()).isEqualTo("Farther Business");
        assertThat(response.businesses().get(0).distance())
            .isLessThan(response.businesses().get(1).distance());
    }
}
