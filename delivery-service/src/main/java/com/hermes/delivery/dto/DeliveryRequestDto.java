package com.hermes.delivery.dto;

import com.hermes.common.address.AddressDto;
import com.hermes.delivery.parcel.dto.ParcelDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DeliveryRequestDto(
        @NotNull Long senderProfileId,
        @NotNull Long recipientProfileId,
        @NotNull @Valid AddressDto pickUpAddress,
        @NotNull @Valid AddressDto dropOffAddress,
        @NotEmpty @Valid List<ParcelDto> parcels
) {}