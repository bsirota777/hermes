package com.hermes.delivery.dto;

import com.hermes.parcel.dto.ParcelDto;
import com.hermes.user.dto.AddressDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateDeliveryRequest(
        @NotBlank @Email String recipientEmail,
        @NotNull @Valid AddressDto pickUpAddress,
        @NotNull @Valid AddressDto dropOffAddress,
        @NotBlank String senderPhoneNumber,
        @NotBlank String recipientPhoneNumber,
        @NotEmpty @Valid List<ParcelDto> parcels
) {}