package com.hermes.delivery.mapper;

import com.hermes.delivery.Delivery;
import com.hermes.delivery.dto.DeliveryDto;

public class DeliveryMapper {

    public static DeliveryDto toDto(Delivery delivery) {
        DeliveryDto dto = new DeliveryDto();
        dto.setId(delivery.getId());
        dto.setStatus(delivery.getStatus());
        dto.setCreatedAt(delivery.getCreatedAt());

        dto.setSenderId(delivery.getSender().getUser().getId());
        dto.setSenderName(delivery.getSender().getUser().getName());

        dto.setRecipientId(delivery.getRecipient().getUser().getId());
        dto.setRecipientName(delivery.getRecipient().getUser().getName());

        if (delivery.getDriver() != null) {
            dto.setDriverId(delivery.getDriver().getUser().getId());
            dto.setDriverName(delivery.getDriver().getUser().getName());
        }

        return dto;
    }
}
