package com.proximityservice.business.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximityservice.business.repository.BusinessRepository;
import com.proximityservice.business.repository.GeospatialIndexRepository;
import com.proximityservice.common.dto.BusinessCreateRequest;
import com.proximityservice.common.dto.BusinessDto;
import com.proximityservice.common.dto.BusinessUpdateRequest;
import com.proximityservice.common.geohash.GeoHashUtil;
import com.proximityservice.common.model.Business;
import com.proximityservice.common.model.GeospatialIndex;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class BusinessService {

    private static final long CACHE_TTL_HOURS = 1;

    private final BusinessRepository businessRepository;
    private final GeospatialIndexRepository geospatialIndexRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public BusinessService(BusinessRepository businessRepository,
                           GeospatialIndexRepository geospatialIndexRepository,
                           StringRedisTemplate redisTemplate) {
        this.businessRepository = businessRepository;
        this.geospatialIndexRepository = geospatialIndexRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public BusinessDto getBusiness(Long id) {
        Business business = businessRepository.findById(id)
            .orElseThrow(() -> new BusinessNotFoundException("Business not found: " + id));
        return BusinessDto.from(business);
    }

    @Transactional
    public BusinessDto createBusiness(BusinessCreateRequest request) {
        Business business = new Business();
        business.setName(request.name());
        business.setDescription(request.description());
        business.setAddress(request.address());
        business.setCity(request.city());
        business.setState(request.state());
        business.setCountry(request.country());
        business.setZipCode(request.zipCode());
        business.setLatitude(request.latitude());
        business.setLongitude(request.longitude());
        business.setCategory(request.category());
        business.setPhone(request.phone());
        business.setWebsite(request.website());

        Business saved = businessRepository.save(business);
        saveGeospatialIndex(saved);
        invalidateCache(saved);
        return BusinessDto.from(saved);
    }

    @Transactional
    public BusinessDto updateBusiness(Long id, BusinessUpdateRequest request) {
        Business business = businessRepository.findById(id)
            .orElseThrow(() -> new BusinessNotFoundException("Business not found: " + id));

        boolean locationChanged = false;

        if (request.name() != null) business.setName(request.name());
        if (request.description() != null) business.setDescription(request.description());
        if (request.address() != null) business.setAddress(request.address());
        if (request.city() != null) business.setCity(request.city());
        if (request.state() != null) business.setState(request.state());
        if (request.country() != null) business.setCountry(request.country());
        if (request.zipCode() != null) business.setZipCode(request.zipCode());
        if (request.category() != null) business.setCategory(request.category());
        if (request.phone() != null) business.setPhone(request.phone());
        if (request.website() != null) business.setWebsite(request.website());

        if (request.latitude() != null || request.longitude() != null) {
            locationChanged = true;
            if (request.latitude() != null) business.setLatitude(request.latitude());
            if (request.longitude() != null) business.setLongitude(request.longitude());
        }

        if (locationChanged) {
            geospatialIndexRepository.deleteByBusinessId(id);
        }

        Business saved = businessRepository.save(business);

        if (locationChanged) {
            saveGeospatialIndex(saved);
        }

        invalidateCache(saved);
        return BusinessDto.from(saved);
    }

    @Transactional
    public void deleteBusiness(Long id) {
        Business business = businessRepository.findById(id)
            .orElseThrow(() -> new BusinessNotFoundException("Business not found: " + id));
        invalidateCacheForDelete(business);
        geospatialIndexRepository.deleteByBusinessId(id);
        businessRepository.delete(business);
    }

    private void saveGeospatialIndex(Business business) {
        for (int precision : GeoHashUtil.getAllPrecisions()) {
            String geohash = GeoHashUtil.encode(business.getLatitude(), business.getLongitude(), precision);
            geospatialIndexRepository.save(new GeospatialIndex(geohash, business.getId()));
        }
    }

    private void invalidateCache(Business business) {
        try {
            BusinessDto dto = BusinessDto.from(business);
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set("biz:" + business.getId(), json, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            // Log and continue — cache miss is acceptable
        }
        for (int precision : GeoHashUtil.getAllPrecisions()) {
            String geohash = GeoHashUtil.encode(business.getLatitude(), business.getLongitude(), precision);
            redisTemplate.delete("geo:" + geohash);
        }
    }

    private void invalidateCacheForDelete(Business business) {
        redisTemplate.delete("biz:" + business.getId());
        for (int precision : GeoHashUtil.getAllPrecisions()) {
            String geohash = GeoHashUtil.encode(business.getLatitude(), business.getLongitude(), precision);
            redisTemplate.delete("geo:" + geohash);
        }
    }

    public static class BusinessNotFoundException extends RuntimeException {
        public BusinessNotFoundException(String message) {
            super(message);
        }
    }
}
