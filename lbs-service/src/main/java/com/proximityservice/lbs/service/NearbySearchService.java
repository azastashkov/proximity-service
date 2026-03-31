package com.proximityservice.lbs.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximityservice.common.dto.BusinessDto;
import com.proximityservice.common.dto.NearbySearchResponse;
import com.proximityservice.common.geohash.GeoHashUtil;
import com.proximityservice.common.geohash.HaversineUtil;
import com.proximityservice.common.model.Business;
import com.proximityservice.common.model.GeospatialIndex;
import com.proximityservice.lbs.repository.BusinessRepository;
import com.proximityservice.lbs.repository.GeospatialIndexRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class NearbySearchService {

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final GeospatialIndexRepository geoRepository;
    private final BusinessRepository businessRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public NearbySearchService(GeospatialIndexRepository geoRepository,
                                BusinessRepository businessRepository,
                                StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper) {
        this.geoRepository = geoRepository;
        this.businessRepository = businessRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public NearbySearchResponse searchNearby(double latitude, double longitude, double radiusKm) {
        List<String> geohashes = GeoHashUtil.getSearchGeohashes(latitude, longitude, radiusKm);

        Set<Long> businessIds = getBusinessIdsFromGeohashes(geohashes);

        if (businessIds.isEmpty()) {
            return new NearbySearchResponse(List.of(), 0);
        }

        Map<Long, Business> businesses = getBusinesses(businessIds);

        List<BusinessDto> results = businesses.values().stream()
            .map(b -> {
                double distance = HaversineUtil.distanceKm(latitude, longitude, b.getLatitude(), b.getLongitude());
                return BusinessDto.from(b).withDistance(Math.round(distance * 100.0) / 100.0);
            })
            .filter(dto -> dto.distance() <= radiusKm)
            .sorted(Comparator.comparingDouble(BusinessDto::distance))
            .collect(Collectors.toList());

        return new NearbySearchResponse(results, results.size());
    }

    private Set<Long> getBusinessIdsFromGeohashes(List<String> geohashes) {
        Set<Long> businessIds = new HashSet<>();
        List<String> cacheMissGeohashes = new ArrayList<>();

        for (String geohash : geohashes) {
            String cacheKey = "geo:" + geohash;
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    List<Long> ids = objectMapper.readValue(cached, new TypeReference<>() {
                    });
                    businessIds.addAll(ids);
                    continue;
                }
            } catch (Exception e) {
                // Fall through to DB
            }
            cacheMissGeohashes.add(geohash);
        }

        if (!cacheMissGeohashes.isEmpty()) {
            List<GeospatialIndex> entries = geoRepository.findByGeohashIn(cacheMissGeohashes);

            Map<String, List<Long>> byGeohash = new HashMap<>();
            for (GeospatialIndex entry : entries) {
                byGeohash.computeIfAbsent(entry.getGeohash(), k -> new ArrayList<>()).add(entry.getBusinessId());
                businessIds.add(entry.getBusinessId());
            }

            // Populate cache for each queried geohash (including empty ones)
            for (String geohash : cacheMissGeohashes) {
                String cacheKey = "geo:" + geohash;
                List<Long> ids = byGeohash.getOrDefault(geohash, List.of());
                try {
                    String json = objectMapper.writeValueAsString(ids);
                    redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
                } catch (Exception e) {
                    // Ignore cache population errors
                }
            }
        }

        return businessIds;
    }

    private Map<Long, Business> getBusinesses(Set<Long> businessIds) {
        Map<Long, Business> businesses = new HashMap<>();
        List<Long> cacheMissIds = new ArrayList<>();

        for (Long id : businessIds) {
            String cacheKey = "biz:" + id;
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    BusinessDto dto = objectMapper.readValue(cached, BusinessDto.class);
                    Business b = toBusinessFromDto(dto);
                    businesses.put(id, b);
                    continue;
                }
            } catch (Exception e) {
                // Fall through to DB
            }
            cacheMissIds.add(id);
        }

        if (!cacheMissIds.isEmpty()) {
            List<Business> dbBusinesses = businessRepository.findAllById(cacheMissIds);
            for (Business b : dbBusinesses) {
                businesses.put(b.getId(), b);
                String cacheKey = "biz:" + b.getId();
                try {
                    BusinessDto dto = BusinessDto.from(b);
                    String json = objectMapper.writeValueAsString(dto);
                    redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
                } catch (Exception e) {
                    // Ignore cache population errors
                }
            }
        }

        return businesses;
    }

    private Business toBusinessFromDto(BusinessDto dto) {
        Business b = new Business();
        b.setId(dto.id());
        b.setName(dto.name());
        b.setDescription(dto.description());
        b.setAddress(dto.address());
        b.setCity(dto.city());
        b.setState(dto.state());
        b.setCountry(dto.country());
        b.setZipCode(dto.zipCode());
        b.setLatitude(dto.latitude());
        b.setLongitude(dto.longitude());
        b.setCategory(dto.category());
        b.setPhone(dto.phone());
        b.setWebsite(dto.website());
        return b;
    }
}
