package com.hermes.delivery;

import com.hermes.common.profile.DriverProfileSummary;
import com.hermes.common.profile.ProfileSummary;
import com.hermes.common.user.UserSummary;
import com.hermes.delivery.client.ProfileServiceClient;
import com.hermes.delivery.client.UserServiceClient;
import com.hermes.delivery.dto.CompleteDeliveryRequest;
import com.hermes.delivery.dto.CreateDeliveryRequest;
import com.hermes.delivery.dto.DeliveryDto;
import com.hermes.delivery.exception.DriverProfileNotFoundException;
import com.hermes.delivery.exception.UserNotFoundException;
import com.hermes.delivery.mapper.DeliveryMapper;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final UserServiceClient userServiceClient;
    private final ProfileServiceClient profileServiceClient;
    private final DeliveryMapper deliveryMapper;

    public DeliveryController(DeliveryService deliveryService,
                              UserServiceClient userServiceClient,
                              ProfileServiceClient profileServiceClient,
                              DeliveryMapper deliveryMapper) {
        this.deliveryService = deliveryService;
        this.userServiceClient = userServiceClient;
        this.profileServiceClient = profileServiceClient;
        this.deliveryMapper = deliveryMapper;
    }

    @PostMapping
    public ResponseEntity<DeliveryDto> createDelivery(
            @Valid @RequestBody CreateDeliveryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserSummary user = currentUser(userDetails);
        Delivery delivery = deliveryService.createDelivery(user.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(deliveryMapper.toDto(delivery));
    }

    @GetMapping("/sent")
    public Page<DeliveryDto> getMySentDeliveries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserSummary user = currentUser(userDetails);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return deliveryService.getSentDeliveries(user.id(), pageable).map(deliveryMapper::toDto);
    }

    @GetMapping("/received")
    public Page<DeliveryDto> getMyReceivedDeliveries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserSummary user = currentUser(userDetails);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return deliveryService.getReceivedDeliveries(user.id(), pageable).map(deliveryMapper::toDto);
    }

    @GetMapping("/driven")
    public Page<DeliveryDto> getMyDrivenDeliveries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserSummary user = currentUser(userDetails);
        DriverProfileSummary driver = currentDriverProfile(user);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return deliveryService.getDrivenDeliveries(driver.profileId(), pageable).map(deliveryMapper::toDto);
    }

    @GetMapping("/in-transit")
    public Page<DeliveryDto> getInTransitDeliveries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return deliveryService.getInTransitDeliveries(pageable)
                .map(deliveryMapper::toDto); // map entity -> DTO, don't return entities directly from a controller
    }

    @PostMapping("/{deliveryId}/assign")
    public DeliveryDto assignDriver(@PathVariable Long deliveryId,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        UserSummary user = currentUser(userDetails);
        DriverProfileSummary driver = currentDriverProfile(user);

        return deliveryMapper.toDto(deliveryService.reserve(deliveryId, driver.profileId()));
    }

    @PostMapping("/{deliveryId}/start")
    public DeliveryDto startTransit(@PathVariable Long deliveryId,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        // verify the caller is the assigned driver for this delivery
        UserSummary user = currentUser(userDetails);
        Delivery delivery = deliveryService.getById(deliveryId);
        requireAssignedDriver(delivery, user);

        return deliveryMapper.toDto(deliveryService.updateStatus(deliveryId, DeliveryStatus.IN_TRANSIT));
    }

    @PostMapping("/{deliveryId}/deliver")
    public DeliveryDto markDelivered(@PathVariable Long deliveryId,
                                     @Valid @RequestBody CompleteDeliveryRequest request,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        // verify the caller is the assigned driver for this delivery
        UserSummary user = currentUser(userDetails);
        Delivery delivery = deliveryService.getById(deliveryId);
        requireAssignedDriver(delivery, user);

        return deliveryMapper.toDto(deliveryService.completeDelivery(deliveryId, request.qrCodeToken()));
    }

    @PostMapping("/{deliveryId}/cancel")
    public DeliveryDto cancel(@PathVariable Long deliveryId,
                              @AuthenticationPrincipal UserDetails userDetails) {
        UserSummary user = currentUser(userDetails);
        Delivery delivery = deliveryService.getById(deliveryId);

        ProfileSummary sender = profileServiceClient.getSenderProfile(delivery.getSenderId());
        if (!sender.userId().equals(user.id())) {
            throw new AccessDeniedException("Not the sender of this delivery");
        }

        return deliveryMapper.toDto(deliveryService.updateStatus(deliveryId, DeliveryStatus.CANCELLED));
    }

    @GetMapping("/queue")
    public Page<DeliveryDto> getQueue(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserSummary user = currentUser(userDetails);
        DriverProfileSummary driver = currentDriverProfile(user);
        return deliveryService.getQueueForDriver(driver.profileId(), page, size).map(deliveryMapper::toDto);
    }

    @GetMapping("/{deliveryId}/qrcode")
    public ResponseEntity<String> getQrCode(@PathVariable Long deliveryId, @AuthenticationPrincipal UserDetails userDetails) {
        UserSummary caller = currentUser(userDetails);
        Delivery delivery = deliveryService.getById(deliveryId);

        ProfileSummary recipient = profileServiceClient.getRecipientProfile(delivery.getRecipientId());
        if (!recipient.userId().equals(caller.id())) {
            throw new AccessDeniedException("Only the recipient can view this delivery's QR code");
        }
        return ResponseEntity.ok(delivery.getQrCodeToken());
    }

    private UserSummary currentUser(UserDetails userDetails) {
        return userServiceClient.findUserByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException(userDetails.getUsername()));
    }

    private DriverProfileSummary currentDriverProfile(UserSummary user) {
        return profileServiceClient.findDriverProfileByUserId(user.id())
                .orElseThrow(() -> new DriverProfileNotFoundException(user.id()));
    }

    private void requireAssignedDriver(Delivery delivery, UserSummary user) {
        if (delivery.getDriverId() == null) {
            throw new AccessDeniedException("Not the assigned driver for this delivery");
        }
        DriverProfileSummary assignedDriver = profileServiceClient.getDriverProfile(delivery.getDriverId());
        if (!assignedDriver.userId().equals(user.id())) {
            throw new AccessDeniedException("Not the assigned driver for this delivery");
        }
    }
}
