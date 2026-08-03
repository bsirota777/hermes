package com.hermes.user.dto;

public record DriverRegistrationRequest(
        String address,
        String phoneNumber,
        String licenceNumber,
        String vehiclePlate
) {}