package com.hermes.user.dto;

public record AddressDto(
        String streetNumber,
        String streetName,
        String suburb,
        String state,
        String postcode
) {}
