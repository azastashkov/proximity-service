package com.proximityservice.common.dto;

import java.util.List;

public record NearbySearchResponse(List<BusinessDto> businesses, int total) {}
