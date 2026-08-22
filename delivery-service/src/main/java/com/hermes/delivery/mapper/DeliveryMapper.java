package com.hermes.delivery.mapper;

import com.hermes.common.profile.DriverProfileSummary;
import com.hermes.common.profile.ProfileSummary;
import com.hermes.delivery.Delivery;
import com.hermes.delivery.client.ProfileServiceClient;
import com.hermes.delivery.dto.DeliveryDto;
import org.springframework.stereotype.Component;

@Component
public class DeliveryMapper {

    private final ProfileServiceClient profileServiceClient;

    public DeliveryMapper(ProfileServiceClient profileServiceClient) {
        this.profileServiceClient = profileServiceClient;
    }

    public DeliveryDto toDto(Delivery delivery) {
        DeliveryDto dto = new DeliveryDto();
        dto.setId(delivery.getId());
        dto.setStatus(delivery.getStatus());
        dto.setCreatedAt(delivery.getCreatedAt());

        ProfileSummary sender = profileServiceClient.getSenderProfile(delivery.getSenderId());
        dto.setSenderId(sender.userId());
        dto.setSenderName(sender.name());

        ProfileSummary recipient = profileServiceClient.getRecipientProfile(delivery.getRecipientId());
        dto.setRecipientId(recipient.userId());
        dto.setRecipientName(recipient.name());

        if (delivery.getDriverId() != null) {
            DriverProfileSummary driver = profileServiceClient.getDriverProfile(delivery.getDriverId());
            dto.setDriverId(driver.userId());
            dto.setDriverName(driver.name());
        }

        return dto;
    }
}
