package com.hermes.common.address;

import jakarta.validation.constraints.NotBlank;

public record AddressDto(
        @NotBlank String streetNumber,
        @NotBlank String streetName,
        @NotBlank String suburb,
        @NotBlank String state,
        @NotBlank String postcode
) {
    public String toFormattedString() {
        return String.format("%s %s, %s %s %s, Australia", streetNumber, streetName, suburb, state, postcode);
    }
}