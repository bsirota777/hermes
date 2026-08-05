package com.hermes.delivery;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.hermes.delivery.dto.CreateDeliveryRequest;
import com.hermes.delivery.dto.DeliveryRequestDto;
import com.hermes.geocoding.EarthDistanceCalculator;
import com.hermes.parcel.dto.ParcelDto;
import com.hermes.delivery.exception.*;
import com.hermes.geocoding.Coordinates;
import com.hermes.geocoding.GeocodingService;
import com.hermes.parcel.Parcel;
import com.hermes.parcel.ParcelRepository;
import com.hermes.pricing.PricingService;
import com.hermes.user.*;
import com.hermes.user.exception.RecipientProfileNotFoundException;
import com.hermes.user.exception.SenderProfileNotFoundException;
import com.hermes.user.exception.UserNotFoundException;
import com.hermes.wallet.WalletService;
import com.hermes.wallet.WalletTransactionType;
import jakarta.transaction.Transactional;
import org.apache.camel.ProducerTemplate;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    private final GeocodingService geocodingService;
    private final PricingService pricingService; // once built
    private final UserRepository userRepository;

    private static final BigDecimal DEFAULT_DRIVER_COMMISSION_RATE = new BigDecimal("0.80");

    public DeliveryService(DeliveryRepository deliveryRepository, ProducerTemplate producerTemplate,
                           SenderProfileRepository senderProfileRepository,
                           RecipientProfileRepository recipientProfileRepository,
                           ParcelRepository parcelRepository, WalletService walletService,
                           GeocodingService geocodingService, PricingService pricingService,
                           UserRepository userRepository) {
        this.deliveryRepository = deliveryRepository;
        this.producerTemplate = producerTemplate;
        this.senderProfileRepository = senderProfileRepository;
        this.recipientProfileRepository = recipientProfileRepository;
        this.parcelRepository = parcelRepository;
        this.walletService = walletService;
        this.geocodingService = geocodingService;
        this.pricingService = pricingService;
        this.userRepository = userRepository;
    }

    public Delivery getById(Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));
    }

    @Transactional
    public Delivery createDelivery(User senderUser, CreateDeliveryRequest request) {
        User recipientUser = userRepository.findByEmail(request.recipientEmail())
                .orElseThrow(() -> new UserNotFoundException(request.recipientEmail()));

        SenderProfile senderProfile = senderProfileRepository.findByUserId(senderUser.getId())
                .orElseGet(() -> {
                    SenderProfile p = new SenderProfile();
                    p.setUser(senderUser);
                    p.setAddress(request.pickUpAddress().toEntity());
                    p.setPhoneNumber(request.senderPhoneNumber());
                    return senderProfileRepository.save(p);
                });

        RecipientProfile recipientProfile = recipientProfileRepository.findByUserId(recipientUser.getId())
                .orElseGet(() -> {
                    RecipientProfile p = new RecipientProfile();
                    p.setUser(recipientUser);
                    p.setAddress(request.dropOffAddress().toEntity());
                    p.setPhoneNumber(request.recipientPhoneNumber());
                    return recipientProfileRepository.save(p);
                });

        DeliveryRequestDto dto = new DeliveryRequestDto(
                senderProfile.getId(), recipientProfile.getId(),
                request.pickUpAddress(), request.dropOffAddress(), request.parcels());

        return createDeliveryRequest(dto);
    }

    @Transactional
    public Delivery createDeliveryRequest(DeliveryRequestDto request) {
        SenderProfile sender = senderProfileRepository.findById(request.senderProfileId())
                .orElseThrow(() -> new SenderProfileNotFoundException(request.senderProfileId()));
        RecipientProfile recipient = recipientProfileRepository.findById(request.recipientProfileId())
                .orElseThrow(() -> new RecipientProfileNotFoundException(request.recipientProfileId()));

        validateSenderNotRecipient(sender, recipient);

        Coordinates pickUp = geocodingService.geocode(request.pickUpAddress().toFormattedString());
        Coordinates dropOff = geocodingService.geocode(request.dropOffAddress().toFormattedString());
        BigDecimal deliveryFee = pricingService.calculateDeliveryFee(pickUp, dropOff, request.parcels());

        Delivery delivery = new Delivery();
        delivery.setSender(sender);
        delivery.setRecipient(recipient);
        delivery.setPickUpAddress(request.pickUpAddress().toEntity());
        delivery.setDropOffAddress(request.dropOffAddress().toEntity());
        delivery.setPickUpLatitude(pickUp.latitude());
        delivery.setPickUpLongitude(pickUp.longitude());
        delivery.setDropOffLatitude(dropOff.latitude());
        delivery.setDropOffLongitude(dropOff.longitude());
        delivery.setDeliveryFee(deliveryFee);
        delivery.setDriverCommissionRate(DEFAULT_DRIVER_COMMISSION_RATE);
        delivery.setStatus(DeliveryStatus.CREATED);
        delivery.setQrCodeToken(UUID.randomUUID().toString());

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

    public Page<Delivery> getSentDeliveries(Long userId, Pageable pageable) {
        return senderProfileRepository.findByUserId(userId)
                .map(p -> deliveryRepository.findBySenderId(p.getId(), pageable))
                .orElse(Page.empty(pageable));
    }

    public Page<Delivery> getReceivedDeliveries(Long userId, Pageable pageable) {
        return recipientProfileRepository.findByUserId(userId)
                .map(p -> deliveryRepository.findByRecipientId(p.getId(), pageable))
                .orElse(Page.empty(pageable));
    }

    public Page<Delivery> getDrivenDeliveries(Long driverProfileId, Pageable pageable) {
        return deliveryRepository.findByDriverId(driverProfileId, pageable);
    }

    public Page<Delivery> getInTransitDeliveries(Pageable pageable) {
        return deliveryRepository.findByStatus(DeliveryStatus.IN_TRANSIT, pageable);
    }

    public Page<Delivery> getQueueForDriver(DriverProfile driver, int page, int size) {
        List<Delivery> unassigned = deliveryRepository.findByDriverIsNull(Pageable.unpaged()).getContent();
        Coordinates driverCoords = geocodingService.geocode(driver.getAddress().toFormattedString());

        List<Delivery> sorted = unassigned.stream()
                .sorted(Comparator.comparingDouble(d -> distanceFromDriver(driverCoords, d)))
                .toList();

        int from = Math.min(page * size, sorted.size());
        int to = Math.min(from + size, sorted.size());

        return new PageImpl<>(sorted.subList(from, to), PageRequest.of(page, size), sorted.size());
    }

    private double distanceFromDriver(Coordinates driverCoords, Delivery delivery) {
        if (delivery.getPickUpLatitude() == null || delivery.getPickUpLongitude() == null) {
            return Double.MAX_VALUE; // deliveries created before coordinates were captured sort last
        }
        return EarthDistanceCalculator.calculateHaversineDistance(
                driverCoords.latitude(), driverCoords.longitude(),
                delivery.getPickUpLatitude(), delivery.getPickUpLongitude()
        ).doubleValue();
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

    private void payDriver(Delivery delivery) {
        BigDecimal driverCut = delivery.getDeliveryFee()
                .multiply(delivery.getDriverCommissionRate());

        Long driverUserId = delivery.getDriver().getUser().getId();
        walletService.credit(driverUserId, driverCut, WalletTransactionType.EARNING, delivery);
    }

    private void refundSender(Delivery delivery) {
        BigDecimal refundAmount;

        if (delivery.getDriver() == null) {
            refundAmount = delivery.getDeliveryFee();
        } else {
            refundAmount = delivery.getDeliveryFee().multiply(BigDecimal.valueOf(0.80));
        }

        Long senderUserId = delivery.getSender().getUser().getId();
        walletService.credit(senderUserId, refundAmount, WalletTransactionType.REFUND, delivery);
    }

    public Delivery completeDelivery(Long deliveryId, String scannedToken) {
        Delivery delivery = getById(deliveryId);
        if (!delivery.getQrCodeToken().equals(scannedToken)) {
            throw new InvalidQrCodeException(deliveryId);
        }
        return updateStatus(deliveryId, DeliveryStatus.DELIVERED);
    }
}
