package com.hermes.user.dto;

public record DriverProfileDto(
        Long id,
        String address,
        String phoneNumber,
        String licenceNumber,
        String vehiclePlate
) {}