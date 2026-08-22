package com.hermes.delivery.mapper;

import com.hermes.common.address.AddressDto;
import com.hermes.delivery.Address;

public class AddressMapper {

    private AddressMapper() {}

    public static Address toEntity(AddressDto dto) {
        return new Address(
                dto.streetNumber(),
                dto.streetName(),
                dto.suburb(),
                dto.state(),
                dto.postcode()
        );
    }
}
