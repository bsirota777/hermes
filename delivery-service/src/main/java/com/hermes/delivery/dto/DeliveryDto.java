package com.hermes.delivery.dto;

import com.hermes.delivery.DeliveryStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class DeliveryDto {

    private Long id;
    private DeliveryStatus status;
    private LocalDateTime createdAt;

    private Long senderId;
    private String senderName;

    private Long recipientId;
    private String recipientName;

    private Long driverId;
    private String driverName; // null if unassigned

    public DeliveryDto(Long id, DeliveryStatus status, LocalDateTime createdAt,
                       Long senderId, String senderName,
                       Long recipientId, String recipientName,
                       Long driverId, String driverName) {
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientId = recipientId;
        this.recipientName = recipientName;
        this.driverId = driverId;
        this.driverName = driverName;
    }
}