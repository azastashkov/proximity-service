package com.proximityservice.lbs.controller;

import com.proximityservice.common.dto.NearbySearchResponse;
import com.proximityservice.lbs.service.NearbySearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
public class NearbySearchController {

    private static final Set<Double> VALID_RADII = Set.of(0.5, 1.0, 2.0, 5.0, 20.0);

    private final NearbySearchService searchService;

    public NearbySearchController(NearbySearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/v1/search/nearby")
    public NearbySearchResponse searchNearby(
        @RequestParam double latitude,
        @RequestParam double longitude,
        @RequestParam(defaultValue = "5.0") double radius
    ) {
        if (!VALID_RADII.contains(radius)) {
            throw new InvalidRadiusException();
        }
        return searchService.searchNearby(latitude, longitude, radius);
    }

    public static class InvalidRadiusException extends RuntimeException {
        public InvalidRadiusException() {
            super("Invalid radius. Must be one of: 0.5, 1, 2, 5, 20");
        }
    }
}
