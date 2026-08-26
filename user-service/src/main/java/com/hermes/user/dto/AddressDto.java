package com.hermes.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressDto(
        @NotBlank String streetNumber,
        @NotBlank String streetName,
        @NotBlank String suburb,
        @NotBlank String state,
        @NotBlank String postcode
) {}
