package com.hermes.delivery;

import java.math.BigDecimal;
import java.util.UUID;

import com.hermes.delivery.dto.DeliveryDto;
import com.hermes.delivery.dto.DeliveryRequestDto;
import com.hermes.delivery.dto.DeliveryResponseDto;
import com.hermes.delivery.dto.ParcelDto;
import com.hermes.delivery.exception.*;
import com.hermes.delivery.mapper.DeliveryMapper;
import com.hermes.user.*;
import com.hermes.user.exception.RecipientProfileNotFoundException;
import com.hermes.user.exception.SenderProfileNotFoundException;
import com.hermes.wallet.WalletService;
import com.hermes.wallet.WalletTransactionType;
import jakarta.transaction.Transactional;
import org.apache.camel.ProducerTemplate;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final ProducerTemplate producerTemplate;
    private final SenderProfileRepository senderProfileRepository;
    private final RecipientProfileRepository recipientProfileRepository;
    private final ParcelRepository parcelRepository;
    private final WalletService walletService;

    private static final BigDecimal DEFAULT_DRIVER_COMMISSION_RATE = new BigDecimal("0.80");

    public DeliveryService(DeliveryRepository deliveryRepository, ProducerTemplate producerTemplate,
                           SenderProfileRepository senderProfileRepository,
                           RecipientProfileRepository recipientProfileRepository,
                           ParcelRepository parcelRepository, WalletService walletService) {
        this.deliveryRepository = deliveryRepository;
        this.producerTemplate = producerTemplate;
        this.senderProfileRepository = senderProfileRepository;
        this.recipientProfileRepository = recipientProfileRepository;
        this.parcelRepository = parcelRepository;
        this.walletService = walletService;
    }

    @Transactional
    public Delivery createDeliveryRequest(DeliveryRequestDto request) {
        SenderProfile sender = senderProfileRepository.findById(request.senderProfileId())
                .orElseThrow(() -> new SenderProfileNotFoundException(request.senderProfileId()));
        RecipientProfile recipient = recipientProfileRepository.findById(request.recipientProfileId())
                .orElseThrow(() -> new RecipientProfileNotFoundException(request.recipientProfileId()));

        validateSenderNotRecipient(sender, recipient);

        Delivery delivery = new Delivery();
        delivery.setSender(sender);
        delivery.setRecipient(recipient);
        delivery.setPickUpAddress(request.pickUpAddress());
        delivery.setDropOffAddress(request.dropOffAddress());
        delivery.setDeliveryFee(request.deliveryFee());
        delivery.setDriverCommissionRate(DEFAULT_DRIVER_COMMISSION_RATE);
        delivery.setStatus(DeliveryStatus.CREATED);

        Delivery savedDelivery = deliveryRepository.save(delivery);

        for (ParcelDto parcelDetails : request.parcels()) {
            Parcel parcel = createParcel(parcelDetails, savedDelivery);
            parcelRepository.save(parcel);
        }

        producerTemplate.sendBody("seda:delivery-requests", savedDelivery);

        return savedDelivery;
    }

    private static @NonNull Parcel createParcel(ParcelDto parcelDetails, Delivery savedDelivery) {
        Parcel parcel = new Parcel();
        parcel.setDelivery(savedDelivery);
        parcel.setDescription(parcelDetails.description());
        parcel.setLengthCm(parcelDetails.lengthCm());
        parcel.setWidthCm(parcelDetails.widthCm());
        parcel.setHeightCm(parcelDetails.heightCm());
        parcel.setWeightKg(parcelDetails.weightKg());
        parcel.setDeclaredValue(parcelDetails.declaredValue());
        parcel.setInsured(parcelDetails.insured());
        parcel.setInsuredValue(parcelDetails.insuredValue());
        return parcel;
    }

    private void validateSenderNotRecipient(SenderProfile sender, RecipientProfile recipient) {
        if (sender.getUser().getId().equals(recipient.getUser().getId())) {
            throw new InvalidDeliveryException("Sender and recipient cannot be the same user.");
        }
    }
    public Page<Delivery> getInTransitDeliveries(Pageable pageable) {
        return deliveryRepository.findByStatus(DeliveryStatus.IN_TRANSIT, pageable);
    }

    @Transactional
    public Delivery reserve(Long deliveryId, DriverProfile driver) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        if (delivery.getDriver() != null) {
            throw new DeliveryAlreadyAssignedException(deliveryId);
        }
        if (!DeliveryStatusTransitions.isAllowed(delivery.getStatus(), DeliveryStatus.ASSIGNED)) {
            throw new InvalidStatusTransitionException(delivery.getStatus(), DeliveryStatus.ASSIGNED);
        }

        delivery.setDriver(driver);
        delivery.setStatus(DeliveryStatus.ASSIGNED);

        try {
            return deliveryRepository.save(delivery);
        } catch (OptimisticLockingFailureException e) {
            throw new DeliveryAlreadyAssignedException(deliveryId);
        }
    }

    @Transactional
    public Delivery updateStatus(Long deliveryId, DeliveryStatus newStatus) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        if (!DeliveryStatusTransitions.isAllowed(delivery.getStatus(), newStatus)) {
            throw new InvalidStatusTransitionException(delivery.getStatus(), newStatus);
        }

        delivery.setStatus(newStatus);
        Delivery saved = deliveryRepository.save(delivery);

        if (newStatus == DeliveryStatus.DELIVERED) {
            payDriver(saved);
        } else if (newStatus == DeliveryStatus.CANCELLED) {
            refundSender(saved);
        }

        return saved;
    }

    @Transactional
    public DeliveryDto assignDriver(Long deliveryId, DriverProfile driver) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        if (delivery.getStatus() != DeliveryStatus.CREATED) {
            throw new InvalidStatusTransitionException(delivery.getStatus(), DeliveryStatus.ASSIGNED);
        }
        if (delivery.getDriver() != null) {
            throw new DeliveryAlreadyAssignedException(deliveryId);
        }

        delivery.setDriver(driver);
        delivery.setStatus(DeliveryStatus.ASSIGNED);

        Delivery saved = deliveryRepository.save(delivery);
        return DeliveryMapper.toDto(saved);
    }

    private void payDriver(Delivery delivery) {
        BigDecimal driverCut = delivery.getDeliveryFee()
                .multiply(delivery.getDriverCommissionRate());

        Long driverUserId = delivery.getDriver().getUser().getId();
        walletService.credit(driverUserId, driverCut, WalletTransactionType.EARNING, delivery);
    }

    private void refundSender(Delivery delivery) {
        Long senderUserId = delivery.getSender().getUser().getId();
        walletService.credit(senderUserId, delivery.getDeliveryFee(), WalletTransactionType.REFUND, delivery);
    }
}
