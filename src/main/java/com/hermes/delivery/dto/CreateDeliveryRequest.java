package com.hermes.delivery.dto;

import com.hermes.parcel.dto.ParcelDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateDeliveryRequest(
        @NotBlank @Email String recipientEmail,
        @NotBlank String pickUpAddress,
        @NotBlank String dropOffAddress,
        @NotBlank String senderPhoneNumber,
        @NotBlank String recipientPhoneNumber,
        @NotEmpty @Valid List<ParcelDto> parcels
) {}