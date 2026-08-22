package com.hermes.delivery.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.hermes.delivery.DeliveryStatus;
import com.hermes.delivery.parcel.dto.ParcelResponseDto;

public record DeliveryResponseDto(
        Long id,
        DeliveryStatus status,
        Long senderProfileId,
        Long recipientProfileId,
        Long driverProfileId, // null until assigned
        List<ParcelResponseDto> parcels,
        LocalDateTime createdAt
) {}