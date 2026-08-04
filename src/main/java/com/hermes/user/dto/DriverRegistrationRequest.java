package com.hermes.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DriverRegistrationRequest(
        @NotNull @Valid AddressDto address,
        @NotBlank String phoneNumber,
        @NotBlank String licenceNumber,
        @NotBlank String vehiclePlate
) {}