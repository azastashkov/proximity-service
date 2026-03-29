package com.proximityservice.business.service;

import com.proximityservice.business.repository.BusinessRepository;
import com.proximityservice.business.repository.GeospatialIndexRepository;
import com.proximityservice.common.dto.BusinessCreateRequest;
import com.proximityservice.common.dto.BusinessDto;
import com.proximityservice.common.dto.BusinessUpdateRequest;
import com.proximityservice.common.model.Business;
import com.proximityservice.common.model.GeospatialIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessServiceTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private GeospatialIndexRepository geospatialIndexRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BusinessService businessService;

    private Business sampleBusiness;

    @BeforeEach
    void setUp() {
        sampleBusiness = new Business();
        sampleBusiness.setId(1L);
        sampleBusiness.setName("Test Business");
        sampleBusiness.setDescription("A test business");
        sampleBusiness.setAddress("123 Main St");
        sampleBusiness.setCity("San Francisco");
        sampleBusiness.setState("CA");
        sampleBusiness.setCountry("US");
        sampleBusiness.setZipCode("94102");
        sampleBusiness.setLatitude(37.7749);
        sampleBusiness.setLongitude(-122.4194);
        sampleBusiness.setCategory("restaurant");
        sampleBusiness.setPhone("415-555-0100");
        sampleBusiness.setWebsite("http://test.com");
    }

    @Test
    void createBusiness_savesBusinessAndGeospatialIndex() {
        BusinessCreateRequest request = new BusinessCreateRequest(
            "Test Business", "A test business", "123 Main St",
            "San Francisco", "CA", "US", "94102",
            37.7749, -122.4194, "restaurant", "415-555-0100", "http://test.com"
        );

        when(businessRepository.save(any(Business.class))).thenReturn(sampleBusiness);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        BusinessDto result = businessService.createBusiness(request);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Test Business");

        verify(businessRepository, times(1)).save(any(Business.class));

        // Verify 3 geo entries saved (precisions 4, 5, 6)
        ArgumentCaptor<GeospatialIndex> geoCaptor = ArgumentCaptor.forClass(GeospatialIndex.class);
        verify(geospatialIndexRepository, times(3)).save(geoCaptor.capture());
        List<GeospatialIndex> savedGeoEntries = geoCaptor.getAllValues();
        assertThat(savedGeoEntries).hasSize(3);
        // Each has the same businessId
        savedGeoEntries.forEach(g -> assertThat(g.getBusinessId()).isEqualTo(1L));
        // Geohashes should differ (different precisions)
        assertThat(savedGeoEntries.get(0).getGeohash().length()).isEqualTo(4);
        assertThat(savedGeoEntries.get(1).getGeohash().length()).isEqualTo(5);
        assertThat(savedGeoEntries.get(2).getGeohash().length()).isEqualTo(6);
    }

    @Test
    void getBusiness_existingId_returnsBusinessDto() {
        when(businessRepository.findById(1L)).thenReturn(Optional.of(sampleBusiness));

        BusinessDto result = businessService.getBusiness(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Test Business");
        assertThat(result.latitude()).isEqualTo(37.7749);
    }

    @Test
    void getBusiness_nonExistingId_throwsException() {
        when(businessRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> businessService.getBusiness(999L))
            .isInstanceOf(BusinessService.BusinessNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void updateBusiness_updatesFieldsAndReindexes() {
        BusinessUpdateRequest request = new BusinessUpdateRequest(
            "Updated Name", null, null, null, null, null, null,
            40.7128, -74.0060, null, null, null
        );

        when(businessRepository.findById(1L)).thenReturn(Optional.of(sampleBusiness));

        Business updatedBusiness = new Business();
        updatedBusiness.setId(1L);
        updatedBusiness.setName("Updated Name");
        updatedBusiness.setLatitude(40.7128);
        updatedBusiness.setLongitude(-74.0060);
        updatedBusiness.setCategory("restaurant");
        when(businessRepository.save(any(Business.class))).thenReturn(updatedBusiness);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        BusinessDto result = businessService.updateBusiness(1L, request);

        assertThat(result).isNotNull();
        // Verify old geo entries deleted
        verify(geospatialIndexRepository, times(1)).deleteByBusinessId(1L);
        // Verify new geo entries created (3 precisions)
        verify(geospatialIndexRepository, times(3)).save(any(GeospatialIndex.class));
    }

    @Test
    void deleteBusiness_removesBusinessAndIndex() {
        when(businessRepository.findById(1L)).thenReturn(Optional.of(sampleBusiness));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        businessService.deleteBusiness(1L);

        verify(geospatialIndexRepository, times(1)).deleteByBusinessId(1L);
        verify(businessRepository, times(1)).delete(sampleBusiness);
    }
}
