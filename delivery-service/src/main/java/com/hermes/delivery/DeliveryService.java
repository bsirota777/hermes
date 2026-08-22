package com.hermes.delivery;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.hermes.common.profile.DriverProfileSummary;
import com.hermes.common.profile.FindOrCreateProfileRequest;
import com.hermes.common.profile.ProfileSummary;
import com.hermes.common.user.UserSummary;
import com.hermes.common.wallet.CreditWalletRequest;
import com.hermes.common.wallet.WalletTransactionType;
import com.hermes.delivery.client.ProfileServiceClient;
import com.hermes.delivery.client.UserServiceClient;
import com.hermes.delivery.client.WalletServiceClient;
import com.hermes.delivery.dto.CreateDeliveryRequest;
import com.hermes.delivery.dto.DeliveryRequestDto;
import com.hermes.delivery.geocoding.EarthDistanceCalculator;
import com.hermes.delivery.mapper.AddressMapper;
import com.hermes.delivery.parcel.dto.ParcelDto;
import com.hermes.delivery.exception.*;
import com.hermes.delivery.geocoding.Coordinates;
import com.hermes.delivery.geocoding.GeocodingService;
import com.hermes.delivery.parcel.Parcel;
import com.hermes.delivery.parcel.ParcelRepository;
import com.hermes.delivery.pricing.PricingService;
import jakarta.transaction.Transactional;
import org.apache.camel.ProducerTemplate;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final ProducerTemplate producerTemplate;
    private final ParcelRepository parcelRepository;
    private final GeocodingService geocodingService;
    private final PricingService pricingService;
    private final ProfileServiceClient profileServiceClient;
    private final WalletServiceClient walletServiceClient;
    private final UserServiceClient userServiceClient;

    private static final BigDecimal DEFAULT_DRIVER_COMMISSION_RATE = new BigDecimal("0.80");

    public DeliveryService(DeliveryRepository deliveryRepository, ProducerTemplate producerTemplate,
                           ParcelRepository parcelRepository,
                           GeocodingService geocodingService, PricingService pricingService,
                           ProfileServiceClient profileServiceClient, WalletServiceClient walletServiceClient,
                           UserServiceClient userServiceClient) {
        this.deliveryRepository = deliveryRepository;
        this.producerTemplate = producerTemplate;
        this.parcelRepository = parcelRepository;
        this.geocodingService = geocodingService;
        this.pricingService = pricingService;
        this.profileServiceClient = profileServiceClient;
        this.walletServiceClient = walletServiceClient;
        this.userServiceClient = userServiceClient;
    }

    public Delivery getById(Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));
    }

    @Transactional
    public Delivery createDelivery(Long senderUserId, CreateDeliveryRequest request) {
        UserSummary recipientUser = userServiceClient.findUserByEmail(request.recipientEmail())
                .orElseThrow(() -> new UserNotFoundException(request.recipientEmail()));

        ProfileSummary senderProfile = profileServiceClient.findOrCreateSenderProfile(
                new FindOrCreateProfileRequest(senderUserId, request.pickUpAddress(), request.senderPhoneNumber()));

        ProfileSummary recipientProfile = profileServiceClient.findOrCreateRecipientProfile(
                new FindOrCreateProfileRequest(recipientUser.id(), request.dropOffAddress(), request.recipientPhoneNumber()));

        DeliveryRequestDto dto = new DeliveryRequestDto(
                senderProfile.profileId(), recipientProfile.profileId(),
                request.pickUpAddress(), request.dropOffAddress(), request.parcels());

        return createDeliveryRequest(dto);
    }

    @Transactional
    public Delivery createDeliveryRequest(DeliveryRequestDto request) {
        ProfileSummary sender = getSenderProfileOrThrow(request.senderProfileId());
        ProfileSummary recipient = getRecipientProfileOrThrow(request.recipientProfileId());

        validateSenderNotRecipient(sender, recipient);

        Coordinates pickUp = geocodingService.geocode(request.pickUpAddress().toFormattedString());
        Coordinates dropOff = geocodingService.geocode(request.dropOffAddress().toFormattedString());
        BigDecimal deliveryFee = pricingService.calculateDeliveryFee(pickUp, dropOff, request.parcels());

        Delivery delivery = new Delivery();
        delivery.setSenderId(sender.profileId());
        delivery.setRecipientId(recipient.profileId());
        delivery.setPickUpAddress(AddressMapper.toEntity(request.pickUpAddress()));
        delivery.setDropOffAddress(AddressMapper.toEntity(request.dropOffAddress()));
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

    private ProfileSummary getSenderProfileOrThrow(Long senderProfileId) {
        try {
            return profileServiceClient.getSenderProfile(senderProfileId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new SenderProfileNotFoundException(senderProfileId);
        }
    }

    private ProfileSummary getRecipientProfileOrThrow(Long recipientProfileId) {
        try {
            return profileServiceClient.getRecipientProfile(recipientProfileId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new RecipientProfileNotFoundException(recipientProfileId);
        }
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

    private void validateSenderNotRecipient(ProfileSummary sender, ProfileSummary recipient) {
        if (sender.userId().equals(recipient.userId())) {
            throw new InvalidDeliveryException("Sender and recipient cannot be the same user.");
        }
    }

    public Page<Delivery> getSentDeliveries(Long userId, Pageable pageable) {
        return profileServiceClient.findSenderProfileByUserId(userId)
                .map(p -> deliveryRepository.findBySenderId(p.profileId(), pageable))
                .orElse(Page.empty(pageable));
    }

    public Page<Delivery> getReceivedDeliveries(Long userId, Pageable pageable) {
        return profileServiceClient.findRecipientProfileByUserId(userId)
                .map(p -> deliveryRepository.findByRecipientId(p.profileId(), pageable))
                .orElse(Page.empty(pageable));
    }

    public Page<Delivery> getDrivenDeliveries(Long driverProfileId, Pageable pageable) {
        return deliveryRepository.findByDriverId(driverProfileId, pageable);
    }

    public Page<Delivery> getInTransitDeliveries(Pageable pageable) {
        return deliveryRepository.findByStatus(DeliveryStatus.IN_TRANSIT, pageable);
    }

    public Page<Delivery> getQueueForDriver(Long driverProfileId, int page, int size) {
        DriverProfileSummary driver = profileServiceClient.getDriverProfile(driverProfileId);

        List<Delivery> unassigned = deliveryRepository.findByDriverIdIsNull(Pageable.unpaged()).getContent();
        Coordinates driverCoords = new Coordinates(driver.latitude(), driver.longitude());

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
    public Delivery reserve(Long deliveryId, Long driverId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        if (delivery.getDriverId() != null) {
            throw new DeliveryAlreadyAssignedException(deliveryId);
        }
        if (!DeliveryStatusTransitions.isAllowed(delivery.getStatus(), DeliveryStatus.ASSIGNED)) {
            throw new InvalidStatusTransitionException(delivery.getStatus(), DeliveryStatus.ASSIGNED);
        }

        delivery.setDriverId(driverId);
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

        Long driverUserId = profileServiceClient.getDriverProfile(delivery.getDriverId()).userId();
        walletServiceClient.credit(new CreditWalletRequest(
                driverUserId, driverCut, WalletTransactionType.EARNING, delivery.getId()));
    }

    private void refundSender(Delivery delivery) {
        BigDecimal refundAmount;

        if (delivery.getDriverId() == null) {
            refundAmount = delivery.getDeliveryFee();
        } else {
            refundAmount = delivery.getDeliveryFee().multiply(BigDecimal.valueOf(0.80));
        }

        Long senderUserId = profileServiceClient.getSenderProfile(delivery.getSenderId()).userId();
        walletServiceClient.credit(new CreditWalletRequest(
                senderUserId, refundAmount, WalletTransactionType.REFUND, delivery.getId()));
    }

    public Delivery completeDelivery(Long deliveryId, String scannedToken) {
        Delivery delivery = getById(deliveryId);
        if (!delivery.getQrCodeToken().equals(scannedToken)) {
            throw new InvalidQrCodeException(deliveryId);
        }
        return updateStatus(deliveryId, DeliveryStatus.DELIVERED);
    }
}
