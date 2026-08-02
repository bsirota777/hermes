package com.hermes.admin;

import com.hermes.delivery.DeliveryStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminDeliveryDto(
        Long id,
        DeliveryStatus status,
        String senderName,
        String senderPhone,
        String recipientName,
        String recipientPhone,
        String driverName,      // null if unassigned
        boolean driverVerified, // true if driver has a licenceNumber on file
        String pickUpAddress,
        String dropOffAddress,
        BigDecimal deliveryFee,
        int parcelCount,
        LocalDateTime createdAt
) {}