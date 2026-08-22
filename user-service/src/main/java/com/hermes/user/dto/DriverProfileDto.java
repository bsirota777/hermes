package com.hermes.user.dto;

public record DriverProfileDto(
        Long id,
        Long userId,
        AddressDto address,
        String phoneNumber,
        String licenceNumber,
        String vehiclePlate,
        Double latitude,
        Double longitude
) {}
