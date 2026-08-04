package com.hermes.user.dto;

public record DriverProfileDto(
        Long id,
        AddressDto address,
        String phoneNumber,
        String licenceNumber,
        String vehiclePlate
) {}