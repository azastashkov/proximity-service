package com.proximityservice.common.dto;

public record BusinessUpdateRequest(
    String name, String description, String address, String city,
    String state, String country, String zipCode,
    Double latitude, Double longitude, String category,
    String phone, String website
) {}
