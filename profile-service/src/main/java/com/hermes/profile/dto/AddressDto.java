package com.hermes.user.dto;

import com.hermes.user.Address;
import jakarta.validation.constraints.NotBlank;

public record AddressDto(
        @NotBlank String streetNumber,
        @NotBlank String streetName,
        @NotBlank String suburb,
        @NotBlank String state,
        @NotBlank String postcode
) {
    public String toFormattedString() {
        System.out.println("toFormattedString ****");
        return String.format("%s %s, %s %s %s, Australia", streetNumber, streetName, suburb, state, postcode);
    }

    public Address toEntity() {
        return new Address(streetNumber, streetName, suburb, state, postcode);
    }

    public static AddressDto from(Address address) {
        return new AddressDto(address.getStreetNumber(), address.getStreetName(),
                address.getSuburb(), address.getState(), address.getPostcode());
    }
}