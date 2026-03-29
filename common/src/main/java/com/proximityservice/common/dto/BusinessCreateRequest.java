package com.proximityservice.common.dto;

public record BusinessCreateRequest(
    String name, String description, String address, String city,
    String state, String country, String zipCode,
    double latitude, double longitude, String category,
    String phone, String website
) {}
