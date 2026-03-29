package com.proximityservice.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proximityservice.common.model.Business;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BusinessDto(
    Long id, String name, String description, String address,
    String city, String state, String country, String zipCode,
    double latitude, double longitude, String category,
    String phone, String website, Double distance
) {
    public static BusinessDto from(Business b) {
        return new BusinessDto(b.getId(), b.getName(), b.getDescription(), b.getAddress(),
            b.getCity(), b.getState(), b.getCountry(), b.getZipCode(),
            b.getLatitude(), b.getLongitude(), b.getCategory(),
            b.getPhone(), b.getWebsite(), null);
    }

    public BusinessDto withDistance(double dist) {
        return new BusinessDto(id, name, description, address, city, state, country, zipCode,
            latitude, longitude, category, phone, website, dist);
    }
}
